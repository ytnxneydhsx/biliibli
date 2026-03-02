package com.bilibili.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bilibili.model.dto.PageQueryDTO;
import com.bilibili.model.vo.UserSearchVO;
import com.bilibili.model.vo.VideoVO;

import java.util.List;

public interface SearchService {

    List<VideoVO> searchVideos(String keyword, Long categoryId, PageQueryDTO pageQuery);

    IPage<UserSearchVO> searchUsers(String nickname, String timeOrder, PageQueryDTO pageQuery);

    void recordVideoSearchHistory(Long uid, String keyword);

    List<String> listVideoSearchHistory(Long uid);
}
