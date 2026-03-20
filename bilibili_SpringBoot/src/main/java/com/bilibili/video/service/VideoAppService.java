package com.bilibili.video.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.model.vo.VideoDetailVO;
import com.bilibili.video.model.vo.VideoRankVO;
import com.bilibili.video.model.vo.VideoVO;

public interface VideoAppService {

    IPage<VideoVO> listVideos(PageQueryDTO pageQuery);

    VideoDetailVO getVideoDetail(Long videoId, Long currentUid);

    IPage<VideoRankVO> listVideoRank(PageQueryDTO pageQuery);

    void increaseViewCount(Long videoId);
}
