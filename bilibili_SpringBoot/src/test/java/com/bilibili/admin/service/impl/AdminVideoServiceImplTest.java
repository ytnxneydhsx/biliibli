package com.bilibili.admin.service.impl;

import com.bilibili.admin.mapper.AdminVideoMapper;
import com.bilibili.admin.model.vo.AdminPendingVideoVO;
import com.bilibili.common.enums.RecordStatus;
import com.bilibili.common.result.CursorPageVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVideoServiceImplTest {

    @Mock
    private AdminVideoMapper adminVideoMapper;

    @InjectMocks
    private AdminVideoServiceImpl adminVideoService;

    @Test
    void listDeletedVideos_shouldUseDeletedStatusAndCursorPagination() {
        List<AdminPendingVideoVO> rows = buildRows(100L, 21);
        when(adminVideoMapper.selectVideosByCursor(RecordStatus.DELETED.code(), null, 21)).thenReturn(rows);

        CursorPageVO<AdminPendingVideoVO> page = adminVideoService.listDeletedVideos(null);

        Assertions.assertEquals(20, page.getRecords().size());
        Assertions.assertTrue(page.isHasMore());
        Assertions.assertEquals(81L, page.getNextCursor());
        Assertions.assertEquals(100L, page.getRecords().get(0).getId());
        Assertions.assertEquals(81L, page.getRecords().get(19).getId());
        verify(adminVideoMapper).selectVideosByCursor(RecordStatus.DELETED.code(), null, 21);
    }

    @Test
    void listPublishedVideos_shouldUseNormalStatusAndStopAtEnd() {
        List<AdminPendingVideoVO> rows = buildRows(55L, 3);
        when(adminVideoMapper.selectVideosByCursor(RecordStatus.NORMAL.code(), 9L, 21)).thenReturn(rows);

        CursorPageVO<AdminPendingVideoVO> page = adminVideoService.listPublishedVideos(9L);

        Assertions.assertEquals(3, page.getRecords().size());
        Assertions.assertFalse(page.isHasMore());
        Assertions.assertNull(page.getNextCursor());
        Assertions.assertEquals(55L, page.getRecords().get(0).getId());
        verify(adminVideoMapper).selectVideosByCursor(RecordStatus.NORMAL.code(), 9L, 21);
    }

    private List<AdminPendingVideoVO> buildRows(long startId, int size) {
        List<AdminPendingVideoVO> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            AdminPendingVideoVO row = new AdminPendingVideoVO();
            row.setId(startId - i);
            rows.add(row);
        }
        return rows;
    }
}
