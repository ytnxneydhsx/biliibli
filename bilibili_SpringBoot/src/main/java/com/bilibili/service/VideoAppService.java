package com.bilibili.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.model.dto.PageQueryDTO;
import com.bilibili.model.vo.VideoDetailVO;
import com.bilibili.model.vo.VideoRankVO;
import com.bilibili.model.vo.VideoVO;

public interface VideoAppService {

    IPage<VideoVO> listVideos(PageQueryDTO pageQuery);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    IPage<VideoRankVO> listVideoRank(PageQueryDTO pageQuery);

    void increaseViewCount(Long videoId);
}
