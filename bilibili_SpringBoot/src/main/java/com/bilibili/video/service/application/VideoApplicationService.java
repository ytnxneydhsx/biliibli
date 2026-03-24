package com.bilibili.video.service.application;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoRankVO;
import com.bilibili.video.model.vo.VideoVO;

public interface VideoApplicationService {

    IPage<VideoVO> listVideos(PageQueryDTO pageQuery);

    IPage<VideoVO> listPublishedVideos(Long uid, String title, PageQueryDTO pageQuery);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    IPage<VideoRankVO> listVideoRank(PageQueryDTO pageQuery);

    void increaseViewCount(Long videoId);

    void likeVideo(Long uid, Long videoId);

    void unlikeVideo(Long uid, Long videoId);
}
