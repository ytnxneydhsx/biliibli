package com.bilibili.service;

import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;

import java.util.List;

public interface VideoAppService {

    List<VideoVO> listVideos(Integer pageNo, Integer pageSize);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    List<VideoRankVO> listVideoRank(Integer pageNo, Integer pageSize);

    void increaseViewCount(Long videoId);
}
