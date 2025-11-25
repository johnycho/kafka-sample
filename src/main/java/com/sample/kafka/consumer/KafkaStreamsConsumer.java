package com.sample.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaStreamsConsumer {

    @KafkaListener(topics = "output-topic", groupId = "output-group")
    public void listenOutput(@Payload String message,
                            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                            @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("✅ [대문자변환 결과] Topic: {}, Offset: {}, Message: {}", topic, offset, message);
    }

    @KafkaListener(topics = "filter-output-topic", groupId = "filter-output-group")
    public void listenFilterOutput(@Payload String message,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("✅ [필터링 결과] Topic: {}, Offset: {}, Message: {}", topic, offset, message);
    }

    @KafkaListener(topics = "word-count-output-topic", groupId = "word-count-output-group")
    public void listenWordCountOutput(@Payload String message,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                      @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("✅ [단어카운트 결과] Topic: {}, Offset: {}, 단어: {}, 카운트: {}", topic, offset, key, message);
    }
}


