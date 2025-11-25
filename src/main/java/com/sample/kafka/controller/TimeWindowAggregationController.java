package com.sample.kafka.controller;

import com.sample.kafka.producer.KafkaProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시간 윈도우 집계 API", description = "시간별/일별 집계 및 실시간 이벤트 카운팅 API")
@RestController
@RequestMapping("/api/time-window")
@RequiredArgsConstructor
public class TimeWindowAggregationController {

    private final KafkaProducer kafkaProducer;

    @Operation(
        summary = "시간별 매출 집계 (1시간 단위)",
        description = "상품별 매출을 1시간 단위로 실시간 집계합니다. Key: 상품명, Value: 금액"
    )
    @PostMapping("/hourly-sales")
    public ResponseEntity<String> recordHourlySales(
            @Parameter(description = "상품명", required = true, example = "노트북")
            @RequestParam String product,
            @Parameter(description = "판매 금액", required = true, example = "1500000")
            @RequestParam Long amount) {
        kafkaProducer.sendMessage("hourly-sales-topic", product, amount.toString());
        return ResponseEntity.ok(String.format("시간별 매출 기록 완료 - 상품: %s, 금액: %d원", product, amount));
    }

    @Operation(
        summary = "일별 매출 집계 (1일 단위)",
        description = "카테고리별 매출을 1일 단위로 실시간 집계합니다. Key: 카테고리, Value: 금액"
    )
    @PostMapping("/daily-sales")
    public ResponseEntity<String> recordDailySales(
            @Parameter(description = "카테고리", required = true, example = "전자제품")
            @RequestParam String category,
            @Parameter(description = "판매 금액", required = true, example = "500000")
            @RequestParam Long amount) {
        kafkaProducer.sendMessage("daily-sales-topic", category, amount.toString());
        return ResponseEntity.ok(String.format("일별 매출 기록 완료 - 카테고리: %s, 금액: %d원", category, amount));
    }

    @Operation(
        summary = "실시간 이벤트 카운팅 (5분 단위)",
        description = "이벤트 타입별 발생 횟수를 5분 단위로 실시간 카운팅합니다."
    )
    @PostMapping("/event")
    public ResponseEntity<String> recordEvent(
            @Parameter(description = "이벤트 타입", required = true, example = "USER_LOGIN")
            @RequestParam String eventType,
            @Parameter(description = "이벤트 데이터", required = false, example = "user-123")
            @RequestParam(required = false, defaultValue = "") String data) {
        kafkaProducer.sendMessage("event-topic", eventType, data);
        return ResponseEntity.ok(String.format("이벤트 기록 완료 - 타입: %s, 데이터: %s", eventType, data));
    }

    @Operation(
        summary = "시간별 매출 대량 생성 (테스트용)",
        description = "여러 상품의 매출을 한번에 생성하여 시간별 집계를 테스트합니다."
    )
    @PostMapping("/hourly-sales/bulk")
    public ResponseEntity<String> bulkHourlySales() {
        // 노트북 매출 3건
        kafkaProducer.sendMessage("hourly-sales-topic", "노트북", "1500000");
        kafkaProducer.sendMessage("hourly-sales-topic", "노트북", "1800000");
        kafkaProducer.sendMessage("hourly-sales-topic", "노트북", "2000000");
        
        // 마우스 매출 2건
        kafkaProducer.sendMessage("hourly-sales-topic", "마우스", "50000");
        kafkaProducer.sendMessage("hourly-sales-topic", "마우스", "80000");
        
        // 키보드 매출 2건
        kafkaProducer.sendMessage("hourly-sales-topic", "키보드", "120000");
        kafkaProducer.sendMessage("hourly-sales-topic", "키보드", "150000");
        
        return ResponseEntity.ok("시간별 매출 데이터 7건 생성 완료\n" +
                "- 노트북: 3건 (5,300,000원)\n" +
                "- 마우스: 2건 (130,000원)\n" +
                "- 키보드: 2건 (270,000원)\n" +
                "콘솔에서 실시간 집계 결과를 확인하세요!");
    }

    @Operation(
        summary = "일별 매출 대량 생성 (테스트용)",
        description = "여러 카테고리의 매출을 한번에 생성하여 일별 집계를 테스트합니다."
    )
    @PostMapping("/daily-sales/bulk")
    public ResponseEntity<String> bulkDailySales() {
        // 전자제품 매출 3건
        kafkaProducer.sendMessage("daily-sales-topic", "전자제품", "1500000");
        kafkaProducer.sendMessage("daily-sales-topic", "전자제품", "800000");
        kafkaProducer.sendMessage("daily-sales-topic", "전자제품", "2200000");
        
        // 의류 매출 2건
        kafkaProducer.sendMessage("daily-sales-topic", "의류", "150000");
        kafkaProducer.sendMessage("daily-sales-topic", "의류", "200000");
        
        // 식품 매출 3건
        kafkaProducer.sendMessage("daily-sales-topic", "식품", "50000");
        kafkaProducer.sendMessage("daily-sales-topic", "식품", "30000");
        kafkaProducer.sendMessage("daily-sales-topic", "식품", "70000");
        
        return ResponseEntity.ok("일별 매출 데이터 8건 생성 완료\n" +
                "- 전자제품: 3건 (4,500,000원)\n" +
                "- 의류: 2건 (350,000원)\n" +
                "- 식품: 3건 (150,000원)\n" +
                "콘솔에서 실시간 집계 결과를 확인하세요!");
    }

    @Operation(
        summary = "이벤트 대량 생성 (테스트용)",
        description = "다양한 이벤트를 생성하여 5분 단위 카운팅을 테스트합니다."
    )
    @PostMapping("/event/bulk")
    public ResponseEntity<String> bulkEvents() {
        // 로그인 이벤트 5건
        for (int i = 1; i <= 5; i++) {
            kafkaProducer.sendMessage("event-topic", "USER_LOGIN", "user-" + i);
        }
        
        // 페이지뷰 이벤트 10건
        for (int i = 1; i <= 10; i++) {
            kafkaProducer.sendMessage("event-topic", "PAGE_VIEW", "/product/" + i);
        }
        
        // 구매 이벤트 3건
        for (int i = 1; i <= 3; i++) {
            kafkaProducer.sendMessage("event-topic", "PURCHASE", "order-" + i);
        }
        
        return ResponseEntity.ok("이벤트 데이터 18건 생성 완료\n" +
                "- USER_LOGIN: 5건\n" +
                "- PAGE_VIEW: 10건\n" +
                "- PURCHASE: 3건\n" +
                "콘솔에서 5분 단위 집계 결과를 확인하세요!");
    }
}

