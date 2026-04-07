package com.bilibili.im.mq.consumer.group;

import com.bilibili.im.app.GroupConversationWindowApplicationService;
import com.bilibili.im.conversation.model.enums.ConversationType;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class GroupConversationRedisProjectionConsumer {

    private final GroupConversationWindowApplicationService groupConversationWindowApplicationService;

    public GroupConversationRedisProjectionConsumer(GroupConversationWindowApplicationService groupConversationWindowApplicationService) {
        this.groupConversationWindowApplicationService = groupConversationWindowApplicationService;
    }

    @RabbitListener(queues = "#{@imMqProperties.groupConversationRedisProjectionQueue}")
    public void consume(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        if (!Integer.valueOf(ConversationType.GROUP.getCode()).equals(event.getConversationType())) {
            return;
        }
        groupConversationWindowApplicationService.projectGroupConversationCardToRedis(
                event.getReceiverId(),
                buildConversationSummary(event.getContent()),
                event.getSendTime(),
                event.getServerMessageId()
        );
    }

    private String buildConversationSummary(MessageContentDTO content) {
        if (content == null) {
            return "";
        }

        String text = content.getText() == null ? "" : content.getText().trim();
        List<String> imageUrls = content.getImageUrls() == null ? Collections.emptyList() : content.getImageUrls();
        boolean hasText = !text.isEmpty();
        boolean hasImages = !imageUrls.isEmpty();

        if (hasText && hasImages) {
            return text + " [图片]";
        }
        if (hasText) {
            return text;
        }
        if (hasImages) {
            return "[图片]";
        }
        return "";
    }
}
