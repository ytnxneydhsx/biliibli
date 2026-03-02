package com.bilibili.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;

public interface VideoAppService {

    IPage<VideoVO> listVideos(Integer pageNo, Integer pageSize);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    IPage<VideoRankVO> listVideoRank(Integer pageNo, Integer pageSize);

    void increaseViewCount(Long videoId);
}
