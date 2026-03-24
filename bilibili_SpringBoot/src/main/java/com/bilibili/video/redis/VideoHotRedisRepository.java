package com.bilibili.video.redis;

import com.bilibili.config.properties.VideoHotProperties;
import com.bilibili.video.model.hot.VideoHotCardCache;
import com.bilibili.video.model.vo.VideoVO;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Repository
public class VideoHotRedisRepository {

    private static final String SLOT_A = "a";
    private static final String SLOT_B = "b";
    private static final String SCOPE_TOP = "top";
    private static final String SCOPE_EPHEMERAL = "ephemeral";

    private final StringRedisTemplate stringRedisTemplate;
    private final VideoHotProperties videoHotProperties;

    public VideoHotRedisRepository(StringRedisTemplate stringRedisTemplate,
                                   VideoHotProperties videoHotProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.videoHotProperties = videoHotProperties;
    }

    public String getActiveSlot() {
        String slot = stringRedisTemplate.opsForValue().get(VideoRedisKeys.ACTIVE_SLOT_KEY);
        if (SLOT_A.equals(slot) || SLOT_B.equals(slot)) {
            return slot;
        }
        return SLOT_A;
    }

    public String getStandbySlot(String activeSlot) {
        return SLOT_A.equals(activeSlot) ? SLOT_B : SLOT_A;
    }

    public void setActiveSlot(String slot) {
        stringRedisTemplate.opsForValue().set(VideoRedisKeys.ACTIVE_SLOT_KEY, slot);
    }

    public boolean isWriteFrozen() {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(VideoRedisKeys.WRITE_FROZEN_KEY));
    }

    public void freezeWrites() {
        stringRedisTemplate.opsForValue().set(VideoRedisKeys.WRITE_FROZEN_KEY, "1");
    }

    public void unfreezeWrites() {
        stringRedisTemplate.delete(VideoRedisKeys.WRITE_FROZEN_KEY);
    }

    public VideoHotCardCache loadCard(String slot, Long videoId) {
        Map<Object, Object> raw = stringRedisTemplate.opsForHash().entries(VideoRedisKeys.cardKey(slot, videoId));
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return toCard(raw);
    }

    public Map<Long, VideoHotCardCache> loadCards(String slot, List<Long> videoIds) {
        if (videoIds == null || videoIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, VideoHotCardCache> result = new LinkedHashMap<>();
        for (Long videoId : videoIds) {
            VideoHotCardCache card = loadCard(slot, videoId);
            if (card != null) {
                result.put(videoId, card);
            }
        }
        return result;
    }

    public void saveCard(String slot, VideoHotCardCache card) {
        if (card == null || card.getId() == null) {
            return;
        }
        Map<String, String> values = new HashMap<>();
        values.put("id", String.valueOf(card.getId()));
        values.put("authorUid", nullableLong(card.getAuthorUid()));
        values.put("title", nullableString(card.getTitle()));
        values.put("coverUrl", nullableString(card.getCoverUrl()));
        values.put("viewCount", nullableLong(card.getViewCount()));
        values.put("duration", nullableLong(card.getDuration()));
        values.put("createTime", card.getCreateTime() == null ? "" : card.getCreateTime().toString());
        values.put("nickname", nullableString(card.getNickname()));
        values.put("lastViewAt", nullableLong(card.getLastViewAt()));
        values.put("scope", nullableString(card.getScope()));
        String cardKey = VideoRedisKeys.cardKey(slot, card.getId());
        stringRedisTemplate.opsForHash().putAll(cardKey, values);
        stringRedisTemplate.opsForSet().add(VideoRedisKeys.cardIndexKey(slot), String.valueOf(card.getId()));
    }

    public void saveCards(String slot, List<VideoVO> videos, String scope) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        long nowMillis = System.currentTimeMillis();
        for (VideoVO video : videos) {
            VideoHotCardCache card = fromVideoVO(video, scope, nowMillis);
            saveCard(slot, card);
        }
    }

    public List<Long> loadRankIds(String slot, long start, long end) {
        Set<String> values = stringRedisTemplate.opsForZSet().reverseRange(VideoRedisKeys.rankKey(slot), start, end);
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (String value : values) {
            Long parsed = parseLong(value);
            if (parsed != null) {
                result.add(parsed);
            }
        }
        return result;
    }

    public Map<Long, Double> loadRankScores(String slot, long start, long end) {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(VideoRedisKeys.rankKey(slot), start, end);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> result = new LinkedHashMap<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple == null || tuple.getValue() == null) {
                continue;
            }
            Long videoId = parseLong(tuple.getValue());
            if (videoId == null) {
                continue;
            }
            result.put(videoId, tuple.getScore() == null ? 0D : tuple.getScore());
        }
        return result;
    }

    public long getRankTotal(String slot) {
        Long total = stringRedisTemplate.opsForZSet().zCard(VideoRedisKeys.rankKey(slot));
        return total == null ? 0L : total;
    }

    public Set<Long> readDirtyVideoIds(String slot) {
        Set<String> members = stringRedisTemplate.opsForSet().members(VideoRedisKeys.dirtyKey(slot));
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (String member : members) {
            Long videoId = parseLong(member);
            if (videoId != null) {
                result.add(videoId);
            }
        }
        return result;
    }

    public void clearSlot(String slot) {
        Set<Long> cardIds = readCardIndex(slot);
        List<String> keys = new ArrayList<>();
        keys.add(VideoRedisKeys.rankKey(slot));
        keys.add(VideoRedisKeys.dirtyKey(slot));
        keys.add(VideoRedisKeys.cardIndexKey(slot));
        for (Long cardId : cardIds) {
            keys.add(VideoRedisKeys.cardKey(slot, cardId));
        }
        if (!keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    public void bootstrapActiveSlot(List<VideoVO> videos) {
        clearSlot(SLOT_A);
        clearSlot(SLOT_B);
        setActiveSlot(SLOT_A);
        if (videos == null || videos.isEmpty()) {
            return;
        }
        saveCards(SLOT_A, videos, SCOPE_TOP);
        for (VideoVO video : videos) {
            if (video.getId() == null) {
                continue;
            }
            double score = video.getViewCount() == null ? 0D : video.getViewCount().doubleValue();
            stringRedisTemplate.opsForZSet().add(VideoRedisKeys.rankKey(SLOT_A), String.valueOf(video.getId()), score);
        }
    }

    public void copyActiveToStandby(String activeSlot) {
        String standbySlot = getStandbySlot(activeSlot);
        clearSlot(standbySlot);

        Set<Long> rankIds = new LinkedHashSet<>(loadRankIds(activeSlot, 0, videoHotProperties.getRankSize() - 1L));
        Map<Long, Double> rankScores = loadRankScores(activeSlot, 0, videoHotProperties.getRankSize() - 1L);
        long now = System.currentTimeMillis();
        for (Long videoId : readCardIndex(activeSlot)) {
            VideoHotCardCache card = loadCard(activeSlot, videoId);
            if (card == null) {
                continue;
            }
            boolean inRank = rankIds.contains(videoId);
            boolean activeWithinWindow = card.getLastViewAt() != null
                    && now - card.getLastViewAt() <= videoHotProperties.getActiveWindowMillis();
            if (!inRank && !activeWithinWindow) {
                continue;
            }
            card.setScope(inRank ? SCOPE_TOP : SCOPE_EPHEMERAL);
            saveCard(standbySlot, card);
            if (inRank) {
                double score = rankScores.getOrDefault(videoId,
                        card.getViewCount() == null ? 0D : card.getViewCount().doubleValue());
                stringRedisTemplate.opsForZSet()
                        .add(VideoRedisKeys.rankKey(standbySlot), String.valueOf(videoId), score);
            }
        }
    }

    private Set<Long> readCardIndex(String slot) {
        Set<String> members = stringRedisTemplate.opsForSet().members(VideoRedisKeys.cardIndexKey(slot));
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (String member : members) {
            Long videoId = parseLong(member);
            if (videoId != null) {
                result.add(videoId);
            }
        }
        return result;
    }

    private static VideoHotCardCache fromVideoVO(VideoVO video, String scope, long nowMillis) {
        VideoHotCardCache card = new VideoHotCardCache();
        card.setId(video.getId());
        card.setAuthorUid(video.getAuthorUid());
        card.setTitle(video.getTitle());
        card.setCoverUrl(video.getCoverUrl());
        card.setViewCount(video.getViewCount() == null ? 0L : video.getViewCount());
        card.setDuration(video.getDuration());
        card.setCreateTime(video.getCreateTime());
        card.setNickname(video.getNickname());
        card.setLastViewAt(nowMillis);
        card.setScope(scope);
        return card;
    }

    private static VideoHotCardCache toCard(Map<Object, Object> raw) {
        VideoHotCardCache card = new VideoHotCardCache();
        card.setId(parseLong(raw.get("id")));
        card.setAuthorUid(parseLong(raw.get("authorUid")));
        card.setTitle(asString(raw.get("title")));
        card.setCoverUrl(asString(raw.get("coverUrl")));
        card.setViewCount(parseLong(raw.get("viewCount")));
        card.setDuration(parseLong(raw.get("duration")));
        String createTime = asString(raw.get("createTime"));
        if (createTime != null && !createTime.isBlank()) {
            card.setCreateTime(LocalDateTime.parse(createTime));
        }
        card.setNickname(asString(raw.get("nickname")));
        card.setLastViewAt(parseLong(raw.get("lastViewAt")));
        card.setScope(asString(raw.get("scope")));
        return card;
    }

    private static String nullableString(String value) {
        return value == null ? "" : value;
    }

    private static String nullableLong(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        return raw.isBlank() ? null : raw;
    }

    private static Long parseLong(Object value) {
        String raw = asString(value);
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
