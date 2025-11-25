package com.sample.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableKafka
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        
        return new KafkaStreamsConfiguration(props);
    }

    /**
     * 스트림 1: 메시지 변환 (대문자로 변환)
     * input-topic -> 대문자 변환 -> output-topic
     */
    @Bean
    public KStream<String, String> kStreamUpperCase(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("input-topic");
        
        stream
            .peek((key, value) -> log.info("[스트림-대문자변환] 입력 - Key: {}, Value: {}", key, value))
            .mapValues(value -> {
                String upperCase = value.toUpperCase();
                log.info("[스트림-대문자변환] 변환 - {} -> {}", value, upperCase);
                return upperCase;
            })
            .to("output-topic");
        
        return stream;
    }

    /**
     * 스트림 2: 메시지 필터링 (특정 키워드 포함된 메시지만)
     * filter-input-topic -> 필터링 -> filter-output-topic
     */
    @Bean
    public KStream<String, String> kStreamFilter(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("filter-input-topic");
        
        stream
            .peek((key, value) -> log.info("[스트림-필터] 입력 - Key: {}, Value: {}", key, value))
            .filter((key, value) -> {
                boolean contains = value.contains("중요");
                log.info("[스트림-필터] 필터링 - Value: {}, 통과: {}", value, contains);
                return contains;
            })
            .to("filter-output-topic");
        
        return stream;
    }

    /**
     * 스트림 3: 단어 카운트 (실시간 집계)
     * word-input-topic -> 단어별 카운트 -> word-count-output-topic
     */
    @Bean
    public KStream<String, String> kStreamWordCount(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("word-input-topic");
        
        stream
            .peek((key, value) -> log.info("[스트림-단어카운트] 입력 - Key: {}, Value: {}", key, value))
            .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
            .groupBy((key, word) -> word)
            .count(Materialized.as("word-counts-store"))
            .toStream()
            .peek((word, count) -> log.info("[스트림-단어카운트] 결과 - 단어: {}, 카운트: {}", word, count))
            .mapValues(String::valueOf)
            .to("word-count-output-topic", Produced.with(Serdes.String(), Serdes.String()));
        
        return stream;
    }

    /**
     * 스트림 4: 시간별 집계 (Tumbling Window - 1시간)
     * hourly-sales-topic -> 1시간 단위 집계 -> hourly-sales-output-topic
     * 
     * 실무 활용 예시:
     * - 시간별 매출 집계
     * - 시간별 방문자 수
     * - 시간별 API 호출 횟수
     */
    @Bean
    public KStream<String, String> kStreamHourlyAggregation(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("hourly-sales-topic");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00")
                .withZone(ZoneId.of("Asia/Seoul"));
        
        stream
            .peek((key, value) -> log.info("[스트림-시간별집계] 입력 - 상품: {}, 금액: {}", key, value))
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .aggregate(
                () -> 0L,  // 초기값
                (key, value, aggregate) -> {
                    try {
                        long amount = Long.parseLong(value);
                        long newTotal = aggregate + amount;
                        log.info("[스트림-시간별집계] 상품: {}, 금액: {}, 누적: {} -> {}", 
                            key, amount, aggregate, newTotal);
                        return newTotal;
                    } catch (NumberFormatException e) {
                        log.warn("[스트림-시간별집계] 숫자 파싱 실패: {}", value);
                        return aggregate;
                    }
                },
                Materialized.with(Serdes.String(), Serdes.Long())
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))  // ← 추가!
            .toStream()
            .map((windowedKey, value) -> {
                String productName = windowedKey.key();
                String windowStart = formatter.format(Instant.ofEpochMilli(windowedKey.window().start()));
                String windowEnd = formatter.format(Instant.ofEpochMilli(windowedKey.window().end()));
                String result = String.format("상품:%s, 시간대:%s~%s, 총매출:%d", 
                    productName, windowStart, windowEnd, value);
                
                log.info("✅ [시간별집계 결과] {}", result);
                return KeyValue.pair(productName, result);
            })
            .to("hourly-sales-output-topic");
        
        return stream;
    }

    /**
     * 스트림 5: 일별 집계 (Tumbling Window - 1일)
     * daily-sales-topic -> 1일 단위 집계 -> daily-sales-output-topic
     * 
     * 실무 활용 예시:
     * - 일별 매출 통계
     * - 일별 사용자 활동량
     * - 일별 주문 건수
     */
    @Bean
    public KStream<String, String> kStreamDailyAggregation(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("daily-sales-topic");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.of("Asia/Seoul"));
        
        stream
            .peek((key, value) -> log.info("[스트림-일별집계] 입력 - 카테고리: {}, 금액: {}", key, value))
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
            .aggregate(
                () -> 0L,
                (key, value, aggregate) -> {
                    try {
                        long amount = Long.parseLong(value);
                        long newTotal = aggregate + amount;
                        log.info("[스트림-일별집계] 카테고리: {}, 금액: {}, 일누적: {} -> {}", 
                            key, amount, aggregate, newTotal);
                        return newTotal;
                    } catch (NumberFormatException e) {
                        log.warn("[스트림-일별집계] 숫자 파싱 실패: {}", value);
                        return aggregate;
                    }
                },
                Materialized.with(Serdes.String(), Serdes.Long())
            )
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))  // ← 추가!
            .toStream()
            .map((windowedKey, value) -> {
                String category = windowedKey.key();
                String date = formatter.format(Instant.ofEpochMilli(windowedKey.window().start()));
                String result = String.format("날짜:%s, 카테고리:%s, 일매출:%d", 
                    date, category, value);
                
                log.info("✅ [일별집계 결과] {}", result);
                return KeyValue.pair(category, result);
            })
            .to("daily-sales-output-topic");
        
        return stream;
    }

    /**
     * 스트림 6: 실시간 이벤트 카운팅 (5분 윈도우)
     * event-topic -> 5분 단위 카운팅 -> event-count-output-topic
     * 
     * 실무 활용 예시:
     * - 실시간 이벤트 모니터링
     * - 5분 단위 트래픽 분석
     * - 이상 탐지 (급격한 증가/감소)
     */
    @Bean
    public KStream<String, String> kStreamEventCounting(StreamsBuilder streamsBuilder) {
        KStream<String, String> stream = streamsBuilder.stream("event-topic");
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.of("Asia/Seoul"));
        
        stream
            .peek((key, value) -> log.info("[스트림-이벤트카운팅] 입력 - 이벤트타입: {}, 데이터: {}", key, value))
            .groupByKey()
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
            .count(Materialized.with(Serdes.String(), Serdes.Long()))
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))  // ← 추가!
            .toStream()
            .map((windowedKey, count) -> {
                String eventType = windowedKey.key();
                String timeStart = formatter.format(Instant.ofEpochMilli(windowedKey.window().start()));
                String timeEnd = formatter.format(Instant.ofEpochMilli(windowedKey.window().end()));
                String result = String.format("이벤트:%s, 시간:%s~%s, 발생횟수:%d", 
                    eventType, timeStart, timeEnd, count);
                
                log.info("✅ [5분간 이벤트 카운트] {}", result);
                return KeyValue.pair(eventType, result);
            })
            .to("event-count-output-topic");
        
        return stream;
    }
}


