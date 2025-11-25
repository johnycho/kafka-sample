package com.sample.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic testTopic() {
        return new NewTopic("test-topic", 1, (short) 1);
    }

    // 카프카 스트림즈용 토픽들
    @Bean
    public NewTopic inputTopic() {
        return new NewTopic("input-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic outputTopic() {
        return new NewTopic("output-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic filterInputTopic() {
        return new NewTopic("filter-input-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic filterOutputTopic() {
        return new NewTopic("filter-output-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic wordInputTopic() {
        return new NewTopic("word-input-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic wordCountOutputTopic() {
        return new NewTopic("word-count-output-topic", 1, (short) 1);
    }

    // 시간별 집계용 토픽
    @Bean
    public NewTopic hourlySalesTopic() {
        return new NewTopic("hourly-sales-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic hourlySalesOutputTopic() {
        return new NewTopic("hourly-sales-output-topic", 1, (short) 1);
    }

    // 일별 집계용 토픽
    @Bean
    public NewTopic dailySalesTopic() {
        return new NewTopic("daily-sales-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic dailySalesOutputTopic() {
        return new NewTopic("daily-sales-output-topic", 1, (short) 1);
    }

    // 이벤트 카운팅용 토픽
    @Bean
    public NewTopic eventTopic() {
        return new NewTopic("event-topic", 1, (short) 1);
    }

    @Bean
    public NewTopic eventCountOutputTopic() {
        return new NewTopic("event-count-output-topic", 1, (short) 1);
    }
}

