package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.config.redis.RedisSearchCacheTuning;
import com.bilibili.config.redis.RedisSearchKeys;
import com.bilibili.mapper.UserMapper;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.model.dto.PageQueryDTO;
import com.bilibili.model.vo.UserSearchVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.SearchService;
import com.bilibili.tool.StringTool;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final int MAX_CANDIDATE_LIMIT = 2000;
    private static final int MIN_CANDIDATE_LIMIT = 200;
    private static final int CANDIDATE_MULTIPLIER = 20;

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoMapper videoMapper;
    private final UserMapper userMapper;

    public SearchServiceImpl(StringRedisTemplate stringRedisTemplate,
                             VideoMapper videoMapper,
                             UserMapper userMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<VideoVO> searchVideos(String keyword, Long categoryId, PageQueryDTO pageQuery) {
        String normalizedKeyword = StringTool.normalizeOptional(keyword);
        Long normalizedCategoryId = normalizeCategoryId(categoryId);
        if (normalizedKeyword == null && normalizedCategoryId == null) {
            throw new IllegalArgumentException("at least one search condition is required");
        }

        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int normalizedPageNo = query.normalizedPageNo();
        int normalizedPageSize = query.normalizedPageSize();
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        int candidateLimit = normalizeCandidateLimit(normalizedPageNo, normalizedPageSize);

        SearchCandidateContext context = new SearchCandidateContext(candidateLimit);
        applyKeywordFilter(context, normalizedKeyword);
        applyCategoryFilter(context, normalizedCategoryId);

        List<Long> candidateIds = context.toOrderedList();
        if (candidateIds.isEmpty() || offset >= candidateIds.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(offset + normalizedPageSize, candidateIds.size());
        List<Long> pageIds = candidateIds.subList(offset, toIndex);
        List<VideoVO> rows = videoMapper.selectPublishedVideosByIds(pageIds);
        return rows == null ? Collections.emptyList() : rows;
    }

    @Override
    public IPage<UserSearchVO> searchUsers(String nickname, String timeOrder, PageQueryDTO pageQuery) {
        String normalizedNickname = StringTool.normalizeRequired(nickname, "nickname");
        boolean desc = normalizeTimeOrder(timeOrder);

        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int normalizedPageNo = query.normalizedPageNo();
        int normalizedPageSize = query.normalizedPageSize();

        Page<UserSearchVO> page = new Page<>(normalizedPageNo, normalizedPageSize);
        return userMapper.selectUsersByNickname(page, normalizedNickname, desc);
    }

    @Override
    public void recordVideoSearchHistory(Long uid, String keyword) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        String normalizedKeyword = StringTool.normalizeOptional(keyword);
        if (normalizedKeyword == null) {
            throw new IllegalArgumentException("keyword is invalid");
        }

        String key = RedisSearchKeys.searchHistoryKey(RedisSearchKeys.DOMAIN_VIDEO, uid);

        stringRedisTemplate.opsForList().leftPush(key, normalizedKeyword);
        stringRedisTemplate.opsForList().trim(key, 0, RedisSearchCacheTuning.SEARCH_HISTORY_MAX_SIZE - 1L);
        stringRedisTemplate.expire(key, RedisSearchCacheTuning.SEARCH_HISTORY_TTL_HOURS, TimeUnit.HOURS);
    }

    @Override
    public List<String> listVideoSearchHistory(Long uid) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        String key = RedisSearchKeys.searchHistoryKey(RedisSearchKeys.DOMAIN_VIDEO, uid);
        List<String> history = stringRedisTemplate.opsForList()
                .range(key, 0, RedisSearchCacheTuning.SEARCH_HISTORY_MAX_SIZE - 1L);
        return history == null ? Collections.emptyList() : history;
    }

    private void applyKeywordFilter(SearchCandidateContext context, String keyword) {
        if (keyword == null) {
            return;
        }
        List<Long> ids = videoMapper.selectPublishedVideoIdsByTitle(keyword, context.getCandidateLimit());
        context.retain(ids);
    }

    private void applyCategoryFilter(SearchCandidateContext context, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        List<Long> ids = videoMapper.selectPublishedVideoIdsByCategoryId(categoryId, context.getCandidateLimit());
        context.retain(ids);
    }

    private static int normalizeCandidateLimit(int pageNo, int pageSize) {
        int requested = pageNo * pageSize * CANDIDATE_MULTIPLIER;
        if (requested < MIN_CANDIDATE_LIMIT) {
            return MIN_CANDIDATE_LIMIT;
        }
        return Math.min(requested, MAX_CANDIDATE_LIMIT);
    }

    private static Long normalizeCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        if (categoryId <= 0) {
            throw new IllegalArgumentException("categoryId is invalid");
        }
        return categoryId;
    }

    private static boolean normalizeTimeOrder(String timeOrder) {
        String normalized = StringTool.normalizeOptional(timeOrder);
        if (normalized == null || "asc".equalsIgnoreCase(normalized)) {
            return false;
        }
        if ("desc".equalsIgnoreCase(normalized)) {
            return true;
        }
        throw new IllegalArgumentException("timeOrder must be asc or desc");
    }

    private static final class SearchCandidateContext {
        private final int candidateLimit;
        private LinkedHashSet<Long> candidateIds;

        private SearchCandidateContext(int candidateLimit) {
            this.candidateLimit = candidateLimit;
        }

        private int getCandidateLimit() {
            return candidateLimit;
        }

        private void retain(List<Long> incomingIds) {
            LinkedHashSet<Long> incoming = toOrderedSet(incomingIds);
            if (candidateIds == null) {
                candidateIds = incoming;
                return;
            }
            if (candidateIds.isEmpty()) {
                return;
            }
            if (incoming.isEmpty()) {
                candidateIds.clear();
                return;
            }

            Set<Long> incomingLookup = incoming;
            candidateIds.removeIf(id -> !incomingLookup.contains(id));
        }

        private List<Long> toOrderedList() {
            if (candidateIds == null || candidateIds.isEmpty()) {
                return Collections.emptyList();
            }
            return candidateIds.stream().collect(Collectors.toList());
        }

        private static LinkedHashSet<Long> toOrderedSet(List<Long> ids) {
            if (ids == null || ids.isEmpty()) {
                return new LinkedHashSet<>();
            }
            return ids.stream()
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
