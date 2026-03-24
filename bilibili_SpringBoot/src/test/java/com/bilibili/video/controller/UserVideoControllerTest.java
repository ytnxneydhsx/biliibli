package com.bilibili.video.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.common.result.Result;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.service.application.VideoApplicationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserVideoControllerTest {

    @Mock
    private VideoApplicationService videoApplicationService;

    @InjectMocks
    private UserVideoController userVideoController;

    @Test
    void listPublishedVideos_shouldReturnPagedData() {
        VideoVO item = new VideoVO();
        item.setId(1L);
        IPage<VideoVO> page = new Page<>(2, 20, 1);
        page.setRecords(Collections.singletonList(item));
        when(videoApplicationService.listPublishedVideos(eq(1001L), eq("test"), argThat(p -> p != null
                && Integer.valueOf(2).equals(p.getPageNo())
                && Integer.valueOf(20).equals(p.getPageSize())))).thenReturn(page);

        PageQueryDTO query = new PageQueryDTO();
        query.setPageNo(2);
        query.setPageSize(20);

        Result<PageVO<VideoVO>> result = userVideoController.listPublishedVideos(1001L, "test", query);

        Assertions.assertEquals(0, result.getCode());
        Assertions.assertEquals(1, result.getData().getRecords().size());
        Assertions.assertEquals(1L, result.getData().getRecords().get(0).getId());
        verify(videoApplicationService).listPublishedVideos(eq(1001L), eq("test"), argThat(p -> p != null
                && Integer.valueOf(2).equals(p.getPageNo())
                && Integer.valueOf(20).equals(p.getPageSize())));
    }
}
