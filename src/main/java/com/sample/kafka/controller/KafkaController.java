package com.sample.kafka.controller;

import com.sample.kafka.producer.KafkaProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "카프카 메시지 API", description = "카프카 메시지 전송을 위한 API")
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaController {

    private final KafkaProducer kafkaProducer;

    @Operation(
        summary = "메시지 전송",
        description = "test-topic으로 단순 메시지를 전송합니다."
    )
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(
            @Parameter(description = "전송할 메시지 내용", required = true, example = "안녕하세요")
            @RequestParam String message) {
        kafkaProducer.sendMessage("test-topic", message);
        return ResponseEntity.ok("메시지가 전송되었습니다: " + message);
    }

    @Operation(
        summary = "키와 함께 메시지 전송",
        description = "test-topic으로 키와 함께 메시지를 전송합니다. 같은 키는 같은 파티션으로 전송됩니다."
    )
    @PostMapping("/send-with-key")
    public ResponseEntity<String> sendMessageWithKey(
            @Parameter(description = "메시지 키 (파티션 결정에 사용)", required = true, example = "user-123")
            @RequestParam String key,
            @Parameter(description = "전송할 메시지 내용", required = true, example = "사용자 데이터")
            @RequestParam String message) {
        kafkaProducer.sendMessage("test-topic", key, message);
        return ResponseEntity.ok("메시지가 전송되었습니다 (Key: " + key + ", Message: " + message + ")");
    }
}

