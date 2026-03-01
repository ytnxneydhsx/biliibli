package com.bilibili.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommentCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content;

    private Long parentId;
}

