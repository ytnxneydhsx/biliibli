package com.bilibili.im.group.mapper;

import com.bilibili.im.group.model.entity.ChatGroupMessageDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

public interface ChatGroupMessageMapper {

    @Insert("""
            INSERT INTO chat_group_message (
                group_id,
                message_id,
                group_message_seq
            ) VALUES (
                #{groupId},
                #{messageId},
                #{groupMessageSeq}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatGroupMessageDO message);
}
