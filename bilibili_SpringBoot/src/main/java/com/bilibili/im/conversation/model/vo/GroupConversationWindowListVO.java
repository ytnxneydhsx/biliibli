package com.bilibili.im.conversation.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class GroupConversationWindowListVO {

    private Long ownerUserId;
    private Integer size;
    private List<GroupConversationWindowVO> records;
}
