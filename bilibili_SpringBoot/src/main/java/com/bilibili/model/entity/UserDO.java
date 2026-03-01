package com.bilibili.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data 
@TableName("t_user") 
public class UserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    
    private String username;

    
    private String password;

    
    @TableLogic
    private Integer status;

    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    
    private LocalDateTime deleteTime;
}
