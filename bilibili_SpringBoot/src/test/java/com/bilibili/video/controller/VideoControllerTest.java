package com.bilibili.video.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.common.result.Result;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.common.page.PageVO;
import com.bilibili.video.model.vo.VideoRankVO;
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
class VideoControllerTest {

    @Mock
    private VideoApplicationService videoApplicationService;

    @InjectMocks
    private VideoController videoController;

    @Test
    void listVideos_shouldReturnPagedData() {
        VideoVO item = new VideoVO();
        item.setId(7L);
        IPage<VideoVO> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(item));
        when(videoApplicationService.listVideos(argThat(p -> p != null
                && Integer.valueOf(1).equals(p.getPageNo())
                && Integer.valueOf(10).equals(p.getPageSize())))).thenReturn(page);

        PageQueryDTO query = new PageQueryDTO();
        query.setPageNo(1);
        query.setPageSize(10);

        Result<PageVO<VideoVO>> result = videoController.listVideos(query);

        Assertions.assertEquals(0, result.getCode());
        Assertions.assertEquals(1, result.getData().getTotal());
        Assertions.assertEquals(7L, result.getData().getRecords().get(0).getId());
        verify(videoApplicationService).listVideos(argThat(p -> p != null
                && Integer.valueOf(1).equals(p.getPageNo())
                && Integer.valueOf(10).equals(p.getPageSize())));
    }

    @Test
    void listVideoRank_shouldReturnPagedData() {
        VideoRankVO item = new VideoRankVO();
        item.setId(10L);
        item.setRank(1);
        IPage<VideoRankVO> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(item));
        when(videoApplicationService.listVideoRank(argThat(p -> p != null
                && Integer.valueOf(1).equals(p.getPageNo())
                && Integer.valueOf(10).equals(p.getPageSize())))).thenReturn(page);

        PageQueryDTO query = new PageQueryDTO();
        query.setPageNo(1);
        query.setPageSize(10);

        Result<PageVO<VideoRankVO>> result = videoController.listVideoRank(query);

        Assertions.assertEquals(0, result.getCode());
        Assertions.assertEquals(1, result.getData().getTotal());
        Assertions.assertEquals(1, result.getData().getRecords().get(0).getRank());
        verify(videoApplicationService).listVideoRank(argThat(p -> p != null
                && Integer.valueOf(1).equals(p.getPageNo())
                && Integer.valueOf(10).equals(p.getPageSize())));
    }
}
