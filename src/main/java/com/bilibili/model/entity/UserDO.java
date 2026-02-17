package com.bilibili.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户核心实体类 (映射数据库表 t_user)
 * 这是一个实体类 (Entity/DO) 的标准示范，你可以参考这个结构去写其它的类。
 */
@Data // Lombok 魔法：自动生成 Getter, Setter, toString 等
@TableName("t_user") // 映射数据库表名
public class UserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 雪花算法 ID
     * ASSIGN_ID 会告诉 MyBatis-Plus：插入时光用内建的雪花算法生成一个长整型 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 登录账号
     */
    private String username;

    /**
     * 加密后的密码
     */
    private String password;

    /**
     * 账号状态 (0:正常, 1:注销/逻辑删除)
     * @TableLogic 开启逻辑删除：以后你调 delete 方法，底层会自动变成 UPDATE status = 1
     */
    @TableLogic
    private Integer status;

    /**
     * 创建时间
     * fill = FieldFill.INSERT 表示在插入数据时，自动填充该字段（需配合全局配置，目前由数据库 DEFAULT 实现）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最后更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 注销时间 (可选)
     */
    private LocalDateTime deleteTime;
}
