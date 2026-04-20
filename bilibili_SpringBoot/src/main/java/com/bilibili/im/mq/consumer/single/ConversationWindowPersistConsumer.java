package com.bilibili.im.mq.consumer.single;

import com.bilibili.common.logging.LogContext;
import com.bilibili.im.app.SingleConversationWindowApplicationService;
import com.bilibili.im.message.cache.ImMqConsumerIdempotencyService;
import com.bilibili.im.message.model.dto.MessageContentDTO;
import com.bilibili.im.mq.ImMqLogContext;
import com.bilibili.im.mq.event.ImMessageDispatchEvent;
import com.bilibili.im.mq.metrics.ImMqConsumerMetrics;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;

import static com.bilibili.im.mq.metrics.ImMqConsumerMetrics.Consumer.SINGLE_CONVERSATION_PERSIST;

@Component
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class ConversationWindowPersistConsumer {

    private static final String DEDUPE_CONSUMER = "single_conversation_persist";

    private final SingleConversationWindowApplicationService singleConversationWindowApplicationService;
    private final ImMqConsumerMetrics imMqConsumerMetrics;
    private final ImMqConsumerIdempotencyService imMqConsumerIdempotencyService;

    public ConversationWindowPersistConsumer(SingleConversationWindowApplicationService singleConversationWindowApplicationService,
                                             ImMqConsumerMetrics imMqConsumerMetrics,
                                             ImMqConsumerIdempotencyService imMqConsumerIdempotencyService) {
        this.singleConversationWindowApplicationService = singleConversationWindowApplicationService;
        this.imMqConsumerMetrics = imMqConsumerMetrics;
        this.imMqConsumerIdempotencyService = imMqConsumerIdempotencyService;
    }

    @RabbitListener(
            queues = "#{@imMqProperties.conversationPersistQueue}",
            containerFactory = "imConversationListenerContainerFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void consume(ImMessageDispatchEvent event,
                        @Header(name = LogContext.TRACE_ID_HEADER, required = false) String traceId,
                        @Header(name = LogContext.UID_HEADER, required = false) String uid) {
        try (LogContext.Scope ignored = ImMqLogContext.open(event, traceId, uid)) {
            imMqConsumerMetrics.record(SINGLE_CONVERSATION_PERSIST, event, () -> {
                if (event == null) {
                    throw new IllegalArgumentException("event is invalid");
                }
                if (!imMqConsumerIdempotencyService.tryAcquire(DEDUPE_CONSUMER, event.getServerMessageId())) {
                    return;
                }
                releaseDedupeOnRollback(event.getServerMessageId());
                try {
                    singleConversationWindowApplicationService.projectSingleMessageToConversationWindows(
                            event.getConversationId(),
                            event.getSenderId(),
                            event.getReceiverId(),
                            buildConversationSummary(event.getContent()),
                            event.getSendTime(),
                            event.getServerMessageId()
                    );
                } catch (RuntimeException | Error ex) {
                    imMqConsumerIdempotencyService.release(DEDUPE_CONSUMER, event.getServerMessageId());
                    throw ex;
                }
            });
        }
    }

    private void releaseDedupeOnRollback(Long serverMessageId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    imMqConsumerIdempotencyService.release(DEDUPE_CONSUMER, serverMessageId);
                }
            }
        });
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
