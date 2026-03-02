package com.bilibili.authorization;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bilibili.common.auth.AuthenticatedUser;
import com.bilibili.mapper.CommentMapper;
import com.bilibili.mapper.VideoUploadTaskMapper;
import com.bilibili.model.entity.CommentDO;
import com.bilibili.model.entity.VideoUploadTaskDO;
import com.bilibili.tool.StringTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("authz")
public class AuthzService {

    private static final int STATUS_NORMAL = 0;

    private final CommentMapper commentMapper;
    private final VideoUploadTaskMapper videoUploadTaskMapper;

    @Autowired
    public AuthzService(CommentMapper commentMapper, VideoUploadTaskMapper videoUploadTaskMapper) {
        this.commentMapper = commentMapper;
        this.videoUploadTaskMapper = videoUploadTaskMapper;
    }

    public boolean canDeleteComment(Authentication authentication, Long commentId) {
        Long currentUid = resolveUid(authentication);
        if (currentUid == null || commentId == null || commentId <= 0) {
            return false;
        }
        CommentDO comment = commentMapper.selectById(commentId);
        return comment != null
                && comment.getStatus() != null
                && comment.getStatus() == STATUS_NORMAL
                && currentUid.equals(comment.getUserId());
    }

    public boolean canAccessUploadTask(Authentication authentication, String uploadId) {
        Long currentUid = resolveUid(authentication);
        String normalizedUploadId = StringTool.normalizeOptional(uploadId);
        if (currentUid == null || normalizedUploadId == null) {
            return false;
        }
        LambdaQueryWrapper<VideoUploadTaskDO> query = new LambdaQueryWrapper<>();
        query.eq(VideoUploadTaskDO::getUploadId, normalizedUploadId);
        VideoUploadTaskDO task = videoUploadTaskMapper.selectOne(query);
        return task != null && currentUid.equals(task.getUserId());
    }

    private Long resolveUid(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser) {
            return ((AuthenticatedUser) principal).getUid();
        }
        return null;
    }
}
