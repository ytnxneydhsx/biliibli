package com.bilibili.model.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data // Lombok 魔法：自动生成 Getter, Setter, toString 等
@TableName("t_user_info") // 映射数据库表名
public class UserInfoDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联的用户 ID
     */
    private Long userId;

    private String nickname;

    private String avatarUrl;

    private String sign;

    /**
     * 在 Entity 中建议使用包装类型 Integer，以更好地处理数据库中的 NULL 情况
     */
    private Integer followingCount;

    private Integer followerCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
