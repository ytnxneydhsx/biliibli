package com.bilibili.im.metrics;

import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ImSendObservation {

    private final ImSendMetrics imSendMetrics;

    public ImSendObservation(ImSendMetrics imSendMetrics) {
        this.imSendMetrics = imSendMetrics;
    }

    public Context startAccept(Long senderId, Long clientMessageId) {
        return new Context(imSendMetrics, senderId, clientMessageId);
    }

    public static final class Context implements AutoCloseable {

        private final ImSendMetrics imSendMetrics;
        private final Long senderId;
        private final Long clientMessageId;
        private final long totalStartNanos = System.nanoTime();
        private long validationNanos;
        private long conversationNanos;
        private long locationNanos;
        private long publishNanos;
        private boolean closed;

        private Context(ImSendMetrics imSendMetrics, Long senderId, Long clientMessageId) {
            this.imSendMetrics = imSendMetrics;
            this.senderId = senderId;
            this.clientMessageId = clientMessageId;
        }

        public void observeValidation(Runnable action) {
            long startNanos = System.nanoTime();
            try {
                action.run();
            } finally {
                validationNanos = System.nanoTime() - startNanos;
                imSendMetrics.recordAcceptValidation(validationNanos);
            }
        }

        public <T> T observeConversation(Supplier<T> action) {
            long startNanos = System.nanoTime();
            try {
                return action.get();
            } finally {
                conversationNanos = System.nanoTime() - startNanos;
                imSendMetrics.recordAcceptConversation(conversationNanos);
            }
        }

        public <T> T observeLocation(Supplier<T> action) {
            long startNanos = System.nanoTime();
            try {
                return action.get();
            } finally {
                locationNanos = System.nanoTime() - startNanos;
                imSendMetrics.recordAcceptLocation(locationNanos);
            }
        }

        public void observePublish(Runnable action) {
            long startNanos = System.nanoTime();
            try {
                action.run();
            } finally {
                publishNanos = System.nanoTime() - startNanos;
                imSendMetrics.recordAcceptPublish(publishNanos);
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            long totalNanos = System.nanoTime() - totalStartNanos;
            imSendMetrics.recordAcceptTotal(totalNanos);
            imSendMetrics.recordSlowAccept(
                    senderId,
                    clientMessageId,
                    totalNanos,
                    validationNanos,
                    conversationNanos,
                    locationNanos,
                    publishNanos
            );
        }
    }
}
