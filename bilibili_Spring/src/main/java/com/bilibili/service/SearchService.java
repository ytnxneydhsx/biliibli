package com.bilibili.service;

import com.bilibili.model.vo.VideoVO;

import java.util.List;

public interface SearchService {

    List<VideoVO> searchVideos(String keyword, Long categoryId, Integer pageNo, Integer pageSize);

    void recordVideoSearchHistory(Long uid, String keyword);

    List<String> listVideoSearchHistory(Long uid);
}
