package com.bilibili.service;

import com.bilibili.model.vo.VideoRankVO;

import java.util.List;

public interface VideoRankService {

    void increaseVideoViewScore(Long videoId, long delta);

    List<VideoRankVO> listVideoViewRank(Integer pageNo, Integer pageSize);
}
