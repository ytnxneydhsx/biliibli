package com.bilibili.config.mq;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.im.mq", name = "enabled", havingValue = "true")
public class ImRabbitListenerContainerConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory imPersistListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.im.mq.listener.persist.concurrency:2}") int concurrency,
            @Value("${app.im.mq.listener.persist.max-concurrency:4}") int maxConcurrency,
            @Value("${app.im.mq.listener.persist.prefetch:20}") int prefetch) {
        return createFactory(configurer, connectionFactory, concurrency, maxConcurrency, prefetch);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory imConversationListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.im.mq.listener.conversation.concurrency:2}") int concurrency,
            @Value("${app.im.mq.listener.conversation.max-concurrency:6}") int maxConcurrency,
            @Value("${app.im.mq.listener.conversation.prefetch:20}") int prefetch) {
        return createFactory(configurer, connectionFactory, concurrency, maxConcurrency, prefetch);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory imRedisProjectionListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.im.mq.listener.redis-projection.concurrency:4}") int concurrency,
            @Value("${app.im.mq.listener.redis-projection.max-concurrency:8}") int maxConcurrency,
            @Value("${app.im.mq.listener.redis-projection.prefetch:100}") int prefetch) {
        return createFactory(configurer, connectionFactory, concurrency, maxConcurrency, prefetch);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory imRealtimePushListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.im.mq.listener.realtime-push.concurrency:2}") int concurrency,
            @Value("${app.im.mq.listener.realtime-push.max-concurrency:4}") int maxConcurrency,
            @Value("${app.im.mq.listener.realtime-push.prefetch:50}") int prefetch) {
        return createFactory(configurer, connectionFactory, concurrency, maxConcurrency, prefetch);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory imGroupPersistListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.im.mq.listener.group-persist.concurrency:1}") int concurrency,
            @Value("${app.im.mq.listener.group-persist.max-concurrency:2}") int maxConcurrency,
            @Value("${app.im.mq.listener.group-persist.prefetch:10}") int prefetch) {
        return createFactory(configurer, connectionFactory, concurrency, maxConcurrency, prefetch);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory imGroupRealtimePushListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Value("${app.im.mq.listener.group-realtime-push.concurrency:2}") int concurrency,
            @Value("${app.im.mq.listener.group-realtime-push.max-concurrency:6}") int maxConcurrency,
            @Value("${app.im.mq.listener.group-realtime-push.prefetch:50}") int prefetch) {
        return createFactory(configurer, connectionFactory, concurrency, maxConcurrency, prefetch);
    }

    private SimpleRabbitListenerContainerFactory createFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            int concurrency,
            int maxConcurrency,
            int prefetch) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setPrefetchCount(prefetch);
        return factory;
    }
}
