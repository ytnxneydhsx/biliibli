package com.bilibili.admin.controller;

import com.bilibili.admin.model.vo.AdminPendingVideoVO;
import com.bilibili.admin.service.AdminVideoService;
import com.bilibili.common.result.CursorPageVO;
import com.bilibili.common.result.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVideoControllerTest {

    @Mock
    private AdminVideoService adminVideoService;

    @InjectMocks
    private AdminVideoController adminVideoController;

    @Test
    void listDeletedVideos_shouldDelegateToService() {
        AdminPendingVideoVO item = new AdminPendingVideoVO();
        item.setId(11L);
        CursorPageVO<AdminPendingVideoVO> page = CursorPageVO.of(List.of(item), 11L, false);
        when(adminVideoService.listDeletedVideos(7L)).thenReturn(page);

        Result<CursorPageVO<AdminPendingVideoVO>> result = adminVideoController.listDeletedVideos(7L);

        Assertions.assertEquals(0, result.getCode());
        Assertions.assertEquals(1, result.getData().getRecords().size());
        Assertions.assertEquals(11L, result.getData().getRecords().get(0).getId());
        verify(adminVideoService).listDeletedVideos(7L);
    }

    @Test
    void listPublishedVideos_shouldDelegateToService() {
        AdminPendingVideoVO item = new AdminPendingVideoVO();
        item.setId(21L);
        CursorPageVO<AdminPendingVideoVO> page = CursorPageVO.of(List.of(item), null, false);
        when(adminVideoService.listPublishedVideos(null)).thenReturn(page);

        Result<CursorPageVO<AdminPendingVideoVO>> result = adminVideoController.listPublishedVideos(null);

        Assertions.assertEquals(0, result.getCode());
        Assertions.assertEquals(1, result.getData().getRecords().size());
        Assertions.assertEquals(21L, result.getData().getRecords().get(0).getId());
        verify(adminVideoService).listPublishedVideos(null);
    }
}
