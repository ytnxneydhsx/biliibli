package com.bilibili.service;

import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoVO;

import java.util.List;

public interface VideoService {

    List<VideoVO> listHomepageVideos(String title, Integer pageNo, Integer pageSize);

    List<VideoVO> searchVideos(String keyword, Integer pageNo, Integer pageSize);

    List<VideoVO> listPublishedVideos(Long uid, String title, Integer pageNo, Integer pageSize);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    void increaseViewCount(Long videoId);

    void likeVideo(Long uid, Long videoId);

    void unlikeVideo(Long uid, Long videoId);
}
