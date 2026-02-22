package com.bilibili.service;

import com.bilibili.model.dto.CommentCreateDTO;
import com.bilibili.model.vo.CommentVO;

import java.util.List;

public interface CommentService {

    Long createComment(Long uid, Long videoId, CommentCreateDTO dto);

    List<CommentVO> listComments(Long videoId, Integer pageNo, Integer pageSize, Long currentUid);

    void deleteComment(Long uid, Long commentId);

    void likeComment(Long uid, Long commentId);

    void unlikeComment(Long uid, Long commentId);
}
