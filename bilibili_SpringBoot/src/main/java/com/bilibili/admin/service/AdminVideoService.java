package com.bilibili.admin.service;

import com.bilibili.admin.model.vo.AdminPendingVideoVO;
import com.bilibili.common.result.CursorPageVO;

public interface AdminVideoService {

    CursorPageVO<AdminPendingVideoVO> listPendingVideos(Long cursor);

    CursorPageVO<AdminPendingVideoVO> listDeletedVideos(Long cursor);

    CursorPageVO<AdminPendingVideoVO> listPublishedVideos(Long cursor);

    void reviewVideo(Long videoId, Integer status);
}
