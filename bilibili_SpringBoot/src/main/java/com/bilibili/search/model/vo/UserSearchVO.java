package com.bilibili.search.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserSearchVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long uid;
    private String nickname;
    private String avatar;
    private String sign;
    private LocalDateTime createTime;
}
