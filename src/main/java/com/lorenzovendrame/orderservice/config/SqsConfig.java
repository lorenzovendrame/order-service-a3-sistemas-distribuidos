package com.lorenzovendrame.orderservice.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import tools.jackson.databind.json.JsonMapper;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;

@Configuration
public class SqsConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().build();
    }

    @Bean
    public SqsMessagingMessageConverter sqsMessagingMessageConverter(JsonMapper jsonMapper) {
        JacksonJsonMessageConverter jacksonConverter = new JacksonJsonMessageConverter(jsonMapper);
        SqsMessagingMessageConverter converter = new SqsMessagingMessageConverter();
        converter.setPayloadMessageConverter(jacksonConverter);
        return converter;
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient,
            SqsMessagingMessageConverter sqsMessagingMessageConverter) {

        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options
                        .queueNotFoundStrategy(QueueNotFoundStrategy.FAIL)
                        .messageConverter(sqsMessagingMessageConverter))
                .build();
    }
}