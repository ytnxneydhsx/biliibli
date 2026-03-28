package com.bilibili.im.message.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class MessageContentDTO {

    private String text;

    private List<String> imageUrls;
}
