package com.bilibili.comment.service;

import com.bilibili.comment.model.dto.CommentCreateDTO;
import com.bilibili.common.page.PageQueryDTO;
import com.bilibili.comment.model.vo.CommentVO;

import java.util.List;

public interface CommentService {

    Long createComment(Long uid, Long videoId, CommentCreateDTO dto);

    List<CommentVO> listComments(Long videoId, PageQueryDTO pageQuery, Long currentUid);

    void deleteComment(Long uid, Long commentId);

    void likeComment(Long uid, Long commentId);

    void unlikeComment(Long uid, Long commentId);
}
