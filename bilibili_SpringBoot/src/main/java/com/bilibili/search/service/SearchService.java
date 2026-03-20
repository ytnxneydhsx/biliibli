package com.bilibili.search.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.search.model.vo.UserSearchVO;
import com.bilibili.video.model.vo.VideoVO;

import java.util.List;

public interface SearchService {

    List<VideoVO> searchVideos(String keyword, Long categoryId, PageQueryDTO pageQuery);

    IPage<UserSearchVO> searchUsers(String nickname, String timeOrder, PageQueryDTO pageQuery);

    void recordVideoSearchHistory(Long uid, String keyword);

    List<String> listVideoSearchHistory(Long uid);
}
