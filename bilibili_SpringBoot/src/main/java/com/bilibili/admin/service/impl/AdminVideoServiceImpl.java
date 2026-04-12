package com.bilibili.admin.service.impl;

import com.bilibili.admin.mapper.AdminVideoMapper;
import com.bilibili.admin.model.vo.AdminPendingVideoVO;
import com.bilibili.admin.service.AdminVideoService;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.common.result.CursorPageVO;
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

    private final AdminVideoMapper adminVideoMapper;

    public AdminVideoServiceImpl(AdminVideoMapper adminVideoMapper) {
        this.adminVideoMapper = adminVideoMapper;
    }

    @Override
    public CursorPageVO<AdminPendingVideoVO> listPendingVideos(Long cursor) {
        return listVideosByStatus(RecordStatus.PENDING.code(), cursor);
    }

    @Override
    public CursorPageVO<AdminPendingVideoVO> listDeletedVideos(Long cursor) {
        return listVideosByStatus(RecordStatus.DELETED.code(), cursor);
    }

    @Override
    public CursorPageVO<AdminPendingVideoVO> listPublishedVideos(Long cursor) {
        return listVideosByStatus(RecordStatus.NORMAL.code(), cursor);
    }

    @Override
    public void reviewVideo(Long videoId, Integer status) {
        if (videoId == null || videoId <= 0) {
            throw new IllegalArgumentException("videoId is invalid");
        }
        if (!ALLOWED_STATUS.contains(status)) {
            throw new IllegalArgumentException("status must be 0 (NORMAL) or 1 (DELETED)");
        }

        int rows = adminVideoMapper.updateVideoStatus(videoId, RecordStatus.PENDING.code(), status);
        if (rows == 0) {
            throw new IllegalArgumentException("video not found or not in pending status");
        }
    }

    private CursorPageVO<AdminPendingVideoVO> listVideosByStatus(int status, Long cursor) {
        List<AdminPendingVideoVO> rows = adminVideoMapper.selectVideosByCursor(status, cursor, PAGE_SIZE + 1);

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
}
