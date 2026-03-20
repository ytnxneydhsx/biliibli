package com.bilibili.video.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.video.model.vo.VideoRankVO;

public interface VideoRankService {

    void increaseVideoViewScore(Long videoId, long delta);

    IPage<VideoRankVO> listVideoViewRank(PageQueryDTO pageQuery);
}
