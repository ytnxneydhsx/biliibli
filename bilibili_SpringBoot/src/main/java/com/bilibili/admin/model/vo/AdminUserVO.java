package com.bilibili.admin.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

@Data
public class AdminUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long uid;

    private String username;

    private Integer roleCode;

    private Integer status;

    private String nickname;

    private String avatar;

    private String sign;

    private boolean likeEnabled;

    private boolean commentEnabled;

    private boolean imMessageSendEnabled;

    private boolean videoUploadEnabled;

    private boolean profileEditEnabled;

    private boolean videoBusinessBanned;
}
