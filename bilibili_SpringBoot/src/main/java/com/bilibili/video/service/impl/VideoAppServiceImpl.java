package com.bilibili.video.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoRankVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.service.VideoAppService;
import com.bilibili.video.service.VideoRankService;
import com.bilibili.video.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VideoAppServiceImpl implements VideoAppService {

    private final VideoService videoService;
    private final VideoRankService videoRankService;

    @Autowired
    public VideoAppServiceImpl(VideoService videoService,
                               VideoRankService videoRankService) {
        this.videoService = videoService;
        this.videoRankService = videoRankService;
    }

    @Override
    public IPage<VideoVO> listVideos(PageQueryDTO pageQuery) {
        return videoService.listHomepageVideos(null, pageQuery);
    }

    @Override
    public VideoDetailVO getVideoDetail(Long videoId, Long currentUid) {
        return videoService.getVideoDetail(videoId, currentUid);
    }

    @Override
    public IPage<VideoRankVO> listVideoRank(PageQueryDTO pageQuery) {
        return videoRankService.listVideoViewRank(pageQuery);
    }

    @Override
    public void increaseViewCount(Long videoId) {
        videoService.validateViewableVideo(videoId);
        videoRankService.increaseVideoViewScore(videoId, 1L);
    }
}
