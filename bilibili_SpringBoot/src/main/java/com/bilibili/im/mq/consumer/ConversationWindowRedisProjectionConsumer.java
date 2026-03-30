package com.bilibili.im.mq.consumer;

import com.bilibili.im.app.ConversationWindowApplicationService;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class ConversationWindowRedisProjectionConsumer {

    private final ConversationWindowApplicationService conversationWindowApplicationService;

    public ConversationWindowRedisProjectionConsumer(ConversationWindowApplicationService conversationWindowApplicationService) {
        this.conversationWindowApplicationService = conversationWindowApplicationService;
    }

    @RabbitListener(queues = "#{@imMqProperties.conversationRedisProjectionQueue}")
    public void consume(ImMessageDispatchEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event is invalid");
        }
        conversationWindowApplicationService.projectSingleMessageToRedisConversationWindows(
                event.getConversationId(),
                event.getSenderId(),
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
