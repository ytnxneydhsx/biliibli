package com.bilibili.video.hot;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.mapper.VideoMapper;
import com.bilibili.video.model.hot.VideoHotCardCache;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoRankVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.redis.queue.VideoFrozenWriteQueue;
import com.bilibili.video.redis.repository.VideoHotLuaRepository;
import com.bilibili.video.redis.repository.VideoHotRedisRepository;
import com.bilibili.video.service.VideoService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class VideoHotFacade {

    private static final String SCOPE_TOP = "top";
    private static final String SCOPE_EPHEMERAL = "ephemeral";

    private final VideoService videoService;
    private final VideoMapper videoMapper;
    private final VideoHotRedisRepository videoHotRedisRepository;
    private final VideoHotLuaRepository videoHotLuaRepository;
    private final VideoFrozenWriteQueue videoFrozenWriteQueue;

    public VideoHotFacade(VideoService videoService,
                          VideoMapper videoMapper,
                          VideoHotRedisRepository videoHotRedisRepository,
                          VideoHotLuaRepository videoHotLuaRepository,
                          VideoFrozenWriteQueue videoFrozenWriteQueue) {
        this.videoService = videoService;
        this.videoMapper = videoMapper;
        this.videoHotRedisRepository = videoHotRedisRepository;
        this.videoHotLuaRepository = videoHotLuaRepository;
        this.videoFrozenWriteQueue = videoFrozenWriteQueue;
    }

    public IPage<VideoVO> listHomeVideos(PageQueryDTO pageQuery) {
        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int pageNo = query.normalizedPageNo();
        int pageSize = query.normalizedPageSize();
        long start = (long) (pageNo - 1) * pageSize;
        long end = start + pageSize - 1;

        String slot = videoHotRedisRepository.getActiveSlot();
        long total = videoHotRedisRepository.getRankTotal(slot);
        List<Long> videoIds = videoHotRedisRepository.loadRankIds(slot, start, end);
        if (videoIds.isEmpty()) {
            return toVideoPage(Collections.emptyList(), pageNo, pageSize, total);
        }

        Map<Long, VideoHotCardCache> cardMap = getOrLoadCards(slot, videoIds, SCOPE_TOP);
        List<VideoVO> records = new ArrayList<>();
        for (Long videoId : videoIds) {
            VideoHotCardCache card = cardMap.get(videoId);
            if (card != null) {
                records.add(toVideoVO(card));
            }
        }
        return toVideoPage(records, pageNo, pageSize, total);
    }

    public VideoDetailVO getVideoDetail(Long videoId, Long currentUid) {
        VideoDetailVO detail = videoService.getVideoDetail(videoId, currentUid);
        String slot = videoHotRedisRepository.getActiveSlot();
        VideoHotCardCache card = videoHotRedisRepository.loadCard(slot, videoId);
        if (card != null && card.getViewCount() != null) {
            detail.setViewCount(card.getViewCount());
        }
        return detail;
    }

    public IPage<VideoRankVO> listVideoRank(PageQueryDTO pageQuery) {
        PageQueryDTO query = pageQuery == null ? new PageQueryDTO() : pageQuery;
        int pageNo = query.normalizedPageNo();
        int pageSize = query.normalizedPageSize();
        long start = (long) (pageNo - 1) * pageSize;
        long end = start + pageSize - 1;

        String slot = videoHotRedisRepository.getActiveSlot();
        long total = videoHotRedisRepository.getRankTotal(slot);
        Map<Long, Double> rankScores = videoHotRedisRepository.loadRankScores(slot, start, end);
        if (rankScores.isEmpty()) {
            return toRankPage(Collections.emptyList(), pageNo, pageSize, total);
        }

        List<Long> orderedIds = new ArrayList<>(rankScores.keySet());
        Map<Long, VideoHotCardCache> cardMap = getOrLoadCards(slot, orderedIds, SCOPE_TOP);
        List<VideoRankVO> records = new ArrayList<>();
        int rank = (int) start + 1;
        for (Long videoId : orderedIds) {
            VideoHotCardCache card = cardMap.get(videoId);
            if (card == null) {
                continue;
            }
            VideoRankVO vo = new VideoRankVO();
            vo.setRank(rank++);
            vo.setScore(rankScores.getOrDefault(videoId, 0D));
            vo.setId(card.getId());
            vo.setAuthorUid(card.getAuthorUid());
            vo.setTitle(card.getTitle());
            vo.setCoverUrl(card.getCoverUrl());
            vo.setViewCount(card.getViewCount());
            vo.setDuration(card.getDuration());
            vo.setCreateTime(card.getCreateTime());
            vo.setNickname(card.getNickname());
            records.add(vo);
        }
        return toRankPage(records, pageNo, pageSize, total);
    }

    public void increaseViewCount(Long videoId) {
        if (videoHotRedisRepository.isWriteFrozen()) {
            videoFrozenWriteQueue.offer(videoId);
            return;
        }
        writeViewToActive(videoId);
    }

    public void drainFrozenQueue() {
        List<Long> queuedVideoIds = videoFrozenWriteQueue.drainAll();
        for (Long videoId : queuedVideoIds) {
            writeViewToActive(videoId);
        }
    }

    private void writeViewToActive(Long videoId) {
        String slot = videoHotRedisRepository.getActiveSlot();
        ensureCardPresent(slot, videoId, SCOPE_EPHEMERAL);
        videoHotLuaRepository.increaseView(slot, videoId, System.currentTimeMillis());
    }

    private Map<Long, VideoHotCardCache> getOrLoadCards(String slot, List<Long> videoIds, String scope) {
        Map<Long, VideoHotCardCache> existing = new LinkedHashMap<>(videoHotRedisRepository.loadCards(slot, videoIds));
        List<Long> missingIds = new ArrayList<>();
        for (Long videoId : videoIds) {
            if (!existing.containsKey(videoId)) {
                missingIds.add(videoId);
            }
        }
        if (!missingIds.isEmpty()) {
            List<VideoVO> missingVideos = videoMapper.selectPublishedVideosByIds(missingIds);
            videoHotRedisRepository.saveCards(slot, missingVideos, scope);
            existing.putAll(videoHotRedisRepository.loadCards(slot, missingIds));
        }
        return existing;
    }

    private void ensureCardPresent(String slot, Long videoId, String scope) {
        if (videoHotRedisRepository.loadCard(slot, videoId) != null) {
            return;
        }
        List<VideoVO> videos = videoMapper.selectPublishedVideosByIds(List.of(videoId));
        if (videos == null || videos.isEmpty()) {
            return;
        }
        videoHotRedisRepository.saveCards(slot, videos, scope);
    }

    private static VideoVO toVideoVO(VideoHotCardCache card) {
        VideoVO vo = new VideoVO();
        vo.setId(card.getId());
        vo.setAuthorUid(card.getAuthorUid());
        vo.setTitle(card.getTitle());
        vo.setCoverUrl(card.getCoverUrl());
        vo.setViewCount(card.getViewCount());
        vo.setDuration(card.getDuration());
        vo.setCreateTime(card.getCreateTime());
        vo.setNickname(card.getNickname());
        return vo;
    }

    private static IPage<VideoVO> toVideoPage(List<VideoVO> records, int pageNo, int pageSize, long total) {
        Page<VideoVO> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(records);
        return page;
    }

    private static IPage<VideoRankVO> toRankPage(List<VideoRankVO> records, int pageNo, int pageSize, long total) {
        Page<VideoRankVO> page = new Page<>(pageNo, pageSize, total);
        page.setRecords(records);
        return page;
    }
}
