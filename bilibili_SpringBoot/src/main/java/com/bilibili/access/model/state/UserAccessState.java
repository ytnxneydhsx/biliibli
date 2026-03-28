package com.bilibili.access.model.state;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserAccessState {

    private Long userId;

    private boolean likeEnabled;

    private boolean commentEnabled;

    private boolean imMessageSendEnabled;

    private boolean videoUploadEnabled;

    private boolean profileEditEnabled;
}
