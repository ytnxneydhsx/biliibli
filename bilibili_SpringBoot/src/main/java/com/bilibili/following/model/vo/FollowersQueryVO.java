package com.bilibili.following.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;

@Data
public class FollowersQueryVO   implements Serializable{

    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long uid;

    private String nickname;

    private String avatar;

    private String sign;

}
