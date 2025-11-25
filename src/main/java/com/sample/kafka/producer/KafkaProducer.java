package com.sample.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String message) {
        log.info("메시지 전송 시작 - Topic: {}, Message: {}", topic, message);
        
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("메시지 전송 성공 - Topic: {}, Offset: {}, Partition: {}", 
                    topic, 
                    result.getRecordMetadata().offset(),
                    result.getRecordMetadata().partition());
            } else {
                log.error("메시지 전송 실패 - Topic: {}, Message: {}", topic, message, ex);
            }
        });
    }

    public void sendMessage(String topic, String key, String message) {
        log.info("메시지 전송 시작 - Topic: {}, Key: {}, Message: {}", topic, key, message);
        
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("메시지 전송 성공 - Topic: {}, Key: {}, Offset: {}, Partition: {}", 
                    topic, 
                    key,
                    result.getRecordMetadata().offset(),
                    result.getRecordMetadata().partition());
            } else {
                log.error("메시지 전송 실패 - Topic: {}, Key: {}, Message: {}", topic, key, message, ex);
            }
        });
    }
}


