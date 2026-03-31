package com.bilibili.im.message.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class MessageHistoryVO {

    private List<MessageVO> records;
    private Boolean hasMore;
    private Long nextBeforeServerMessageId;
}
