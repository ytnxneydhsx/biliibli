package com.bilibili.service.impl;

import com.bilibili.config.redis.RedisSearchCacheTuning;
import com.bilibili.config.redis.RedisSearchKeys;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.SearchService;
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

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CANDIDATE_LIMIT = 2000;
    private static final int MIN_CANDIDATE_LIMIT = 200;
    private static final int CANDIDATE_MULTIPLIER = 20;

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoMapper videoMapper;

    public SearchServiceImpl(StringRedisTemplate stringRedisTemplate,
                             VideoMapper videoMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.videoMapper = videoMapper;
    }

    @Override
    public List<VideoVO> searchVideos(String keyword, Long categoryId, Integer pageNo, Integer pageSize) {
        String normalizedKeyword = normalizeOptional(keyword);
        Long normalizedCategoryId = normalizeCategoryId(categoryId);
        if (normalizedKeyword == null && normalizedCategoryId == null) {
            throw new IllegalArgumentException("at least one search condition is required");
        }

        int normalizedPageNo = normalizePageNo(pageNo);
        int normalizedPageSize = normalizePageSize(pageSize);
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
    public void recordVideoSearchHistory(Long uid, String keyword) {
        if (uid == null || uid <= 0) {
            throw new IllegalArgumentException("uid is invalid");
        }

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("keyword is invalid");
        }

        String key = RedisSearchKeys.searchHistoryKey(RedisSearchKeys.DOMAIN_VIDEO, uid);
        String normalizedKeyword = keyword.trim();

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

    private static int normalizePageNo(Integer pageNo) {
        if (pageNo == null || pageNo <= 0) {
            return DEFAULT_PAGE_NO;
        }
        return pageNo;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private static int normalizeCandidateLimit(int pageNo, int pageSize) {
        int requested = pageNo * pageSize * CANDIDATE_MULTIPLIER;
        if (requested < MIN_CANDIDATE_LIMIT) {
            return MIN_CANDIDATE_LIMIT;
        }
        return Math.min(requested, MAX_CANDIDATE_LIMIT);
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
