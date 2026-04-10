package com.bilibili.admin.service.impl;

import com.bilibili.admin.model.vo.AdminPendingVideoVO;
import com.bilibili.admin.service.AdminVideoService;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.common.result.CursorPageVO;
import com.bilibili.video.mapper.VideoMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AdminVideoServiceImpl implements AdminVideoService {

    private static final int PAGE_SIZE = 20;
    private static final Set<Integer> ALLOWED_STATUS = Set.of(
            RecordStatus.NORMAL.code(),
            RecordStatus.DELETED.code()
    );

    private final VideoMapper videoMapper;

    public AdminVideoServiceImpl(VideoMapper videoMapper) {
        this.videoMapper = videoMapper;
    }

    @Override
    public CursorPageVO<AdminPendingVideoVO> listPendingVideos(Long cursor) {
        List<AdminPendingVideoVO> rows = videoMapper.selectPendingVideosByCursor(cursor, PAGE_SIZE + 1);

        boolean hasMore = rows.size() > PAGE_SIZE;
        if (hasMore) {
            rows = rows.subList(0, PAGE_SIZE);
        }

        Long nextCursor = null;
        if (hasMore && !rows.isEmpty()) {
            nextCursor = rows.get(rows.size() - 1).getId();
        }

        return CursorPageVO.of(rows, nextCursor, hasMore);
    }

    @Override
    public void reviewVideo(Long videoId, Integer status) {
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }
        if (!ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException("status must be 0 (NORMAL) or 1 (DELETED)");
        }

        int rows = videoMapper.updateVideoStatus(videoId, RecordStatus.PENDING.code(), status);
        if (rows == 0) {
            throw new IllegalArgumentException("video not found or not in pending status");
        }
    }
}
