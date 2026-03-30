package com.bilibili.im.conversation.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConversationWindowListVO {

    private Long ownerUserId;
    private Integer size;
    private List<ConversationWindowVO> records;
}
