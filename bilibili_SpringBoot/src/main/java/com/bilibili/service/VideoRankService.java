package com.bilibili.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.model.vo.VideoRankVO;

public interface VideoRankService {

    void increaseVideoViewScore(Long videoId, long delta);

    IPage<VideoRankVO> listVideoViewRank(Integer pageNo, Integer pageSize);
}
