package com.bilibili.im.group.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ChatGroupMemberListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long groupId;
    private Integer size;
    private List<ChatGroupMemberVO> records;
}
