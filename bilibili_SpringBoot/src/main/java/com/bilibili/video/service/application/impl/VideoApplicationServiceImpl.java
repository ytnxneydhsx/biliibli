package com.bilibili.video.service.application.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoRankVO;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.service.application.VideoApplicationService;
import com.bilibili.video.service.domain.VideoDomainService;
import com.bilibili.video.service.hot.VideoHotFacade;
import org.springframework.stereotype.Service;

@Service
public class VideoApplicationServiceImpl implements VideoApplicationService {

    private final VideoDomainService videoDomainService;
    private final VideoHotFacade videoHotFacade;

    public VideoApplicationServiceImpl(VideoDomainService videoDomainService,
                                       VideoHotFacade videoHotFacade) {
        this.videoDomainService = videoDomainService;
        this.videoHotFacade = videoHotFacade;
    }

    @Override
    public IPage<VideoVO> listVideos(PageQueryDTO pageQuery) {
        return videoHotFacade.listHomeVideos(pageQuery);
    }

    @Override
    public IPage<VideoVO> listPublishedVideos(Long uid, String title, PageQueryDTO pageQuery) {
        return videoDomainService.listPublishedVideos(uid, title, pageQuery);
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
        videoDomainService.validateViewableVideo(videoId);
        videoHotFacade.increaseViewCount(videoId);
    }

    @Override
    public void likeVideo(Long uid, Long videoId) {
        videoDomainService.likeVideo(uid, videoId);
    }

    @Override
    public void unlikeVideo(Long uid, Long videoId) {
        videoDomainService.unlikeVideo(uid, videoId);
    }
}
