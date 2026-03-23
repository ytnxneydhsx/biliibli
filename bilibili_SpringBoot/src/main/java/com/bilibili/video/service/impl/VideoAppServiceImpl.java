package com.bilibili.video.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.hot.VideoHotFacade;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoRankVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.service.VideoAppService;
import com.bilibili.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VideoAppServiceImpl implements VideoAppService {

    private final VideoService videoService;
    private final VideoHotFacade videoHotFacade;

    @Autowired
    public VideoAppServiceImpl(VideoService videoService,
                               VideoHotFacade videoHotFacade) {
        this.videoService = videoService;
        this.videoHotFacade = videoHotFacade;
    }

    @Override
    public IPage<VideoVO> listVideos(PageQueryDTO pageQuery) {
        return videoHotFacade.listHomeVideos(pageQuery);
    }

    @Override
    public VideoDetailVO getVideoDetail(Long videoId, Long currentUid) {
        return videoHotFacade.getVideoDetail(videoId, currentUid);
    }

    @Override
    public IPage<VideoRankVO> listVideoRank(PageQueryDTO pageQuery) {
        return videoHotFacade.listVideoRank(pageQuery);
    }

    @Override
    public void increaseViewCount(Long videoId) {
        videoService.validateViewableVideo(videoId);
        videoHotFacade.increaseViewCount(videoId);
    }
}
