package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.model.dto.PageQueryDTO;
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
