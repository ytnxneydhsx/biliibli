package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;
import com.bilibili.service.VideoAppService;
import com.bilibili.service.VideoRankService;
import com.bilibili.service.VideoService;
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
    public IPage<VideoVO> listVideos(Integer pageNo, Integer pageSize) {
        return videoService.listHomepageVideos(null, pageNo, pageSize);
    }

    @Override
    public VideoDetailVO getVideoDetail(Long videoId, Long currentUid) {
        return videoService.getVideoDetail(videoId, currentUid);
    }

    @Override
    public IPage<VideoRankVO> listVideoRank(Integer pageNo, Integer pageSize) {
        return videoRankService.listVideoViewRank(pageNo, pageSize);
    }

    @Override
    public void increaseViewCount(Long videoId) {
        videoService.validateViewableVideo(videoId);
        videoRankService.increaseVideoViewScore(videoId, 1L);
    }
}
