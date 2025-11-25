package com.sample.kafka.service;

import com.sample.kafka.entity.DailySalesResult;
import com.sample.kafka.entity.EventCountResult;
import com.sample.kafka.entity.HourlySalesResult;
import com.sample.kafka.repository.DailySalesResultRepository;
import com.sample.kafka.repository.EventCountResultRepository;
import com.sample.kafka.repository.HourlySalesResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationStorageService {

    private final HourlySalesResultRepository hourlySalesRepository;
    private final DailySalesResultRepository dailySalesRepository;
    private final EventCountResultRepository eventCountRepository;

    private static final DateTimeFormatter HOURLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DAILY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 시간별 매출 집계 결과를 DB에 저장
     * 형식: "상품:노트북, 시간대:2025-11-13 14:00~2025-11-13 15:00, 총매출:5300000"
     */
    @Transactional
    public void saveHourlySalesResult(String message) {
        try {
            Pattern pattern = Pattern.compile("상품:([^,]+), 시간대:([^~]+)~([^,]+), 총매출:(\\d+)");
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                String productName = matcher.group(1).trim();
                LocalDateTime windowStart = LocalDateTime.parse(matcher.group(2).trim(), HOURLY_FORMATTER);
                LocalDateTime windowEnd = LocalDateTime.parse(matcher.group(3).trim(), HOURLY_FORMATTER);
                Long totalSales = Long.parseLong(matcher.group(4).trim());

                HourlySalesResult result = HourlySalesResult.builder()
                        .productName(productName)
                        .windowStart(windowStart)
                        .windowEnd(windowEnd)
                        .totalSales(totalSales)
                        .build();

                hourlySalesRepository.save(result);
                log.info("💾 [DB 저장 완료] 시간별 매출 - 상품: {}, 매출: {}원", productName, totalSales);
            }
        } catch (Exception e) {
            log.error("시간별 매출 저장 실패: {}", message, e);
        }
    }

    /**
     * 일별 매출 집계 결과를 DB에 저장
     * 형식: "날짜:2025-11-13, 카테고리:전자제품, 일매출:4500000"
     */
    @Transactional
    public void saveDailySalesResult(String message) {
        try {
            Pattern pattern = Pattern.compile("날짜:([^,]+), 카테고리:([^,]+), 일매출:(\\d+)");
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                LocalDate salesDate = LocalDate.parse(matcher.group(1).trim(), DAILY_FORMATTER);
                String category = matcher.group(2).trim();
                Long totalSales = Long.parseLong(matcher.group(3).trim());

                DailySalesResult result = DailySalesResult.builder()
                        .category(category)
                        .salesDate(salesDate)
                        .totalSales(totalSales)
                        .build();

                dailySalesRepository.save(result);
                log.info("💾 [DB 저장 완료] 일별 매출 - 카테고리: {}, 매출: {}원", category, totalSales);
            }
        } catch (Exception e) {
            log.error("일별 매출 저장 실패: {}", message, e);
        }
    }

    /**
     * 이벤트 카운트 결과를 DB에 저장
     * 형식: "이벤트:USER_LOGIN, 시간:14:30~14:35, 발생횟수:5"
     */
    @Transactional
    public void saveEventCountResult(String message) {
        try {
            Pattern pattern = Pattern.compile("이벤트:([^,]+), 시간:([^~]+)~([^,]+), 발생횟수:(\\d+)");
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                String eventType = matcher.group(1).trim();
                String timeStart = matcher.group(2).trim();
                String timeEnd = matcher.group(3).trim();
                Long eventCount = Long.parseLong(matcher.group(4).trim());

                // 시간만 있으므로 오늘 날짜를 붙여서 LocalDateTime 생성
                LocalDate today = LocalDate.now();
                LocalDateTime windowStart = LocalDateTime.parse(today + " " + timeStart, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                LocalDateTime windowEnd = LocalDateTime.parse(today + " " + timeEnd, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                EventCountResult result = EventCountResult.builder()
                        .eventType(eventType)
                        .windowStart(windowStart)
                        .windowEnd(windowEnd)
                        .eventCount(eventCount)
                        .build();

                eventCountRepository.save(result);
                log.info("💾 [DB 저장 완료] 이벤트 카운트 - 타입: {}, 횟수: {}회", eventType, eventCount);
            }
        } catch (Exception e) {
            log.error("이벤트 카운트 저장 실패: {}", message, e);
        }
    }
}

