package com.sample.kafka.consumer;

import com.sample.kafka.service.AggregationStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeWindowConsumer {

    private final AggregationStorageService storageService;

    /**
     * 시간별 매출 집계 결과를 수신하고 DB에 저장
     */
    @KafkaListener(topics = "hourly-sales-output-topic", groupId = "hourly-sales-result-group")
    public void listenHourlySalesResult(@Payload String message,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("🕐 [시간별 집계 최종결과] Topic: {}, Offset: {}, 결과: {}", topic, offset, message);
        
        // DB에 저장
        storageService.saveHourlySalesResult(message);
    }

    /**
     * 일별 매출 집계 결과를 수신하고 DB에 저장
     */
    @KafkaListener(topics = "daily-sales-output-topic", groupId = "daily-sales-result-group")
    public void listenDailySalesResult(@Payload String message,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("📅 [일별 집계 최종결과] Topic: {}, Offset: {}, 결과: {}", topic, offset, message);
        
        // DB에 저장
        storageService.saveDailySalesResult(message);
    }

    /**
     * 이벤트 카운트 결과를 수신하고 DB에 저장
     */
    @KafkaListener(topics = "event-count-output-topic", groupId = "event-count-result-group")
    public void listenEventCountResult(@Payload String message,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("⚡ [이벤트 카운트 최종결과] Topic: {}, Offset: {}, 결과: {}", topic, offset, message);
        
        // DB에 저장
        storageService.saveEventCountResult(message);
    }
}

