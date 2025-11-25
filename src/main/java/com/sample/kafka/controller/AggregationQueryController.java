package com.sample.kafka.controller;

import com.sample.kafka.entity.DailySalesResult;
import com.sample.kafka.entity.EventCountResult;
import com.sample.kafka.entity.HourlySalesResult;
import com.sample.kafka.repository.DailySalesResultRepository;
import com.sample.kafka.repository.EventCountResultRepository;
import com.sample.kafka.repository.HourlySalesResultRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "집계 결과 조회 API", description = "DB에 저장된 집계 결과를 조회하는 API")
@RestController
@RequestMapping("/api/aggregation")
@RequiredArgsConstructor
public class AggregationQueryController {

    private final HourlySalesResultRepository hourlySalesRepository;
    private final DailySalesResultRepository dailySalesRepository;
    private final EventCountResultRepository eventCountRepository;

    @Operation(
        summary = "최근 시간별 매출 조회",
        description = "DB에 저장된 최근 시간별 매출 집계 결과를 조회합니다."
    )
    @GetMapping("/hourly-sales/recent")
    public ResponseEntity<List<HourlySalesResult>> getRecentHourlySales() {
        return ResponseEntity.ok(hourlySalesRepository.findTop10ByOrderByCreatedAtDesc());
    }

    @Operation(
        summary = "상품별 시간별 매출 조회",
        description = "특정 상품의 시간별 매출 집계 결과를 조회합니다."
    )
    @GetMapping("/hourly-sales/by-product")
    public ResponseEntity<List<HourlySalesResult>> getHourlySalesByProduct(
            @Parameter(description = "상품명", required = true, example = "노트북")
            @RequestParam String productName) {
        return ResponseEntity.ok(hourlySalesRepository.findByProductNameOrderByWindowStartDesc(productName));
    }

    @Operation(
        summary = "특정 시간 이후 매출 조회",
        description = "특정 시간 이후의 시간별 매출 집계 결과를 조회합니다."
    )
    @GetMapping("/hourly-sales/since")
    public ResponseEntity<List<HourlySalesResult>> getHourlySalesSince(
            @Parameter(description = "조회 시작 시간", required = true, example = "2025-11-13T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {
        return ResponseEntity.ok(hourlySalesRepository.findRecentResults(startTime));
    }

    @Operation(
        summary = "최근 일별 매출 조회",
        description = "DB에 저장된 최근 일별 매출 집계 결과를 조회합니다."
    )
    @GetMapping("/daily-sales/recent")
    public ResponseEntity<List<DailySalesResult>> getRecentDailySales() {
        return ResponseEntity.ok(dailySalesRepository.findTop10ByOrderByCreatedAtDesc());
    }

    @Operation(
        summary = "카테고리별 일별 매출 조회",
        description = "특정 카테고리의 일별 매출 집계 결과를 조회합니다."
    )
    @GetMapping("/daily-sales/by-category")
    public ResponseEntity<List<DailySalesResult>> getDailySalesByCategory(
            @Parameter(description = "카테고리명", required = true, example = "전자제품")
            @RequestParam String category) {
        return ResponseEntity.ok(dailySalesRepository.findByCategoryOrderBySalesDateDesc(category));
    }

    @Operation(
        summary = "특정 날짜 매출 조회",
        description = "특정 날짜의 일별 매출 집계 결과를 조회합니다."
    )
    @GetMapping("/daily-sales/by-date")
    public ResponseEntity<List<DailySalesResult>> getDailySalesByDate(
            @Parameter(description = "조회 날짜", required = true, example = "2025-11-13")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate) {
        return ResponseEntity.ok(dailySalesRepository.findBySalesDate(salesDate));
    }

    @Operation(
        summary = "최근 이벤트 카운트 조회",
        description = "DB에 저장된 최근 이벤트 카운트 결과를 조회합니다."
    )
    @GetMapping("/event-count/recent")
    public ResponseEntity<List<EventCountResult>> getRecentEventCounts() {
        return ResponseEntity.ok(eventCountRepository.findTop10ByOrderByCreatedAtDesc());
    }

    @Operation(
        summary = "이벤트 타입별 카운트 조회",
        description = "특정 이벤트 타입의 카운트 결과를 조회합니다."
    )
    @GetMapping("/event-count/by-type")
    public ResponseEntity<List<EventCountResult>> getEventCountsByType(
            @Parameter(description = "이벤트 타입", required = true, example = "USER_LOGIN")
            @RequestParam String eventType) {
        return ResponseEntity.ok(eventCountRepository.findByEventTypeOrderByWindowStartDesc(eventType));
    }

    @Operation(
        summary = "전체 집계 통계",
        description = "DB에 저장된 전체 집계 데이터의 통계를 조회합니다."
    )
    @GetMapping("/stats")
    public ResponseEntity<Object> getAggregationStats() {
        long hourlySalesCount = hourlySalesRepository.count();
        long dailySalesCount = dailySalesRepository.count();
        long eventCountCount = eventCountRepository.count();

        return ResponseEntity.ok(new Object() {
            public final String message = "집계 결과 통계";
            public final long 시간별매출집계건수 = hourlySalesCount;
            public final long 일별매출집계건수 = dailySalesCount;
            public final long 이벤트카운트건수 = eventCountCount;
            public final long 총저장건수 = hourlySalesCount + dailySalesCount + eventCountCount;
        });
    }
}

