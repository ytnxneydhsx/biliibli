package com.bilibili.model.vo;
import lombok.Data;

import java.io.Serializable;

@Data
public class FollowersQueryVO   implements Serializable{

    private static final long serialVersionUID = 1L;

    private Long uid;

    private String nickname;

    private String avatar;

    private String sign;

}
