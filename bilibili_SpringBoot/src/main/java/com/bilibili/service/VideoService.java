package com.bilibili.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoVO;

public interface VideoService {

    IPage<VideoVO> listHomepageVideos(String title, Integer pageNo, Integer pageSize);

    IPage<VideoVO> listPublishedVideos(Long uid, String title, Integer pageNo, Integer pageSize);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    void validateViewableVideo(Long videoId);

    void likeVideo(Long uid, Long videoId);

    void unlikeVideo(Long uid, Long videoId);
}
