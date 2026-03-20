package com.bilibili.video.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoVO;

public interface VideoService {

    IPage<VideoVO> listHomepageVideos(String title, PageQueryDTO pageQuery);

    IPage<VideoVO> listPublishedVideos(Long uid, String title, PageQueryDTO pageQuery);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    void validateViewableVideo(Long videoId);

    void likeVideo(Long uid, Long videoId);

    void unlikeVideo(Long uid, Long videoId);
}
