package com.sample.kafka.controller;

import com.sample.kafka.producer.KafkaProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "카프카 스트림즈 API", description = "카프카 스트림즈 테스트를 위한 API")
@RestController
@RequestMapping("/api/kafka-streams")
@RequiredArgsConstructor
public class KafkaStreamsController {

    private final KafkaProducer kafkaProducer;

    @Operation(
        summary = "대문자 변환 스트림 테스트",
        description = "input-topic으로 메시지를 전송하면 스트림이 대문자로 변환하여 output-topic으로 전송합니다."
    )
    @PostMapping("/uppercase")
    public ResponseEntity<String> testUpperCase(
            @Parameter(description = "변환할 메시지", required = true, example = "hello kafka streams")
            @RequestParam String message) {
        kafkaProducer.sendMessage("input-topic", message);
        return ResponseEntity.ok("메시지를 input-topic으로 전송했습니다. 스트림이 대문자로 변환하여 output-topic으로 전송합니다: " + message);
    }

    @Operation(
        summary = "필터링 스트림 테스트",
        description = "filter-input-topic으로 메시지를 전송하면 '중요' 키워드가 포함된 메시지만 filter-output-topic으로 전송됩니다."
    )
    @PostMapping("/filter")
    public ResponseEntity<String> testFilter(
            @Parameter(description = "필터링할 메시지 ('중요' 키워드 포함 시 통과)", required = true, example = "중요한 메시지입니다")
            @RequestParam String message) {
        kafkaProducer.sendMessage("filter-input-topic", message);
        return ResponseEntity.ok("메시지를 filter-input-topic으로 전송했습니다. '중요' 키워드 포함 여부: " + message.contains("중요"));
    }

    @Operation(
        summary = "단어 카운트 스트림 테스트",
        description = "word-input-topic으로 문장을 전송하면 각 단어의 누적 카운트가 word-count-output-topic으로 전송됩니다."
    )
    @PostMapping("/word-count")
    public ResponseEntity<String> testWordCount(
            @Parameter(description = "카운트할 문장", required = true, example = "hello world hello kafka")
            @RequestParam String sentence) {
        kafkaProducer.sendMessage("word-input-topic", sentence);
        return ResponseEntity.ok("문장을 word-input-topic으로 전송했습니다. 각 단어의 카운트가 집계됩니다: " + sentence);
    }

    @Operation(
        summary = "모든 스트림 동시 테스트",
        description = "세 가지 스트림을 모두 동시에 테스트합니다."
    )
    @PostMapping("/test-all")
    public ResponseEntity<String> testAll(
            @Parameter(description = "테스트 메시지", required = true, example = "중요한 Hello World")
            @RequestParam String message) {
        kafkaProducer.sendMessage("input-topic", message);
        kafkaProducer.sendMessage("filter-input-topic", message);
        kafkaProducer.sendMessage("word-input-topic", message);
        
        return ResponseEntity.ok("모든 스트림에 메시지를 전송했습니다:\n" +
                "1. 대문자 변환: input-topic -> output-topic\n" +
                "2. 필터링: filter-input-topic -> filter-output-topic (중요 포함: " + message.contains("중요") + ")\n" +
                "3. 단어 카운트: word-input-topic -> word-count-output-topic");
    }
}


