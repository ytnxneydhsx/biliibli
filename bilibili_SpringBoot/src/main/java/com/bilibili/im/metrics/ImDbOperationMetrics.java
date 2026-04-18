package com.bilibili.im.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ImDbOperationMetrics {

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";

    private final MeterRegistry meterRegistry;

    public ImDbOperationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public <T> T record(String operation, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = STATUS_FAILURE;
        try {
            T result = action.get();
            status = STATUS_SUCCESS;
            return result;
        } catch (RuntimeException | Error ex) {
            Counter.builder("im.db.operation.errors")
                    .description("IM DB operation error count")
                    .tags("operation", operation, "exception", ex.getClass().getSimpleName())
                    .register(meterRegistry)
                    .increment();
            throw ex;
        } finally {
            Tags tags = Tags.of("operation", operation, "status", status);
            Counter.builder("im.db.operation.calls")
                    .description("IM DB operation call count")
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();

            sample.stop(Timer.builder("im.db.operation.duration")
                    .description("IM DB operation duration")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}
