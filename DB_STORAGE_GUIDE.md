# 카프카 Output 토픽 → DB 저장 가이드

## 📊 아키텍처 흐름

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐      ┌──────────┐
│   Input     │      │    Kafka     │      │   Output    │      │          │
│   Topic     │─────▶│   Streams    │─────▶│   Topic     │─────▶│    DB    │
│             │      │  (실시간집계)  │      │             │      │          │
└─────────────┘      └──────────────┘      └─────────────┘      └──────────┘
                                                   │
                                                   │ @KafkaListener
                                                   ▼
                                            ┌─────────────┐
                                            │  Consumer   │
                                            │  + Service  │
                                            └─────────────┘
```

## 🎯 구현 방법 (3가지)

### 1. 커스텀 컨슈머로 직접 저장 ⭐ (이 프로젝트에서 사용)

**장점:**
- 구현이 간단하고 직관적
- 세밀한 제어 가능 (에러 처리, 로깅, 변환 로직 등)
- 비즈니스 로직 추가 용이

**단점:**
- 직접 코드 작성 필요
- 여러 토픽-DB 연동 시 코드 증가

```java
@KafkaListener(topics = "output-topic")
public void listen(String message) {
    // 1. 메시지 파싱
    // 2. 엔티티 변환
    // 3. DB 저장
    repository.save(entity);
}
```

### 2. Kafka Connect (JDBC Sink Connector)

**장점:**
- 코드 없이 설정만으로 구성 가능
- 고성능, 안정적
- 대용량 데이터 처리에 최적

**단점:**
- 별도 Kafka Connect 클러스터 필요
- 설정이 복잡
- 커스터마이징 어려움

```json
{
  "name": "jdbc-sink",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
    "topics": "output-topic",
    "connection.url": "jdbc:postgresql://localhost/mydb",
    "auto.create": "true"
  }
}
```

### 3. 스트림즈 내에서 직접 저장

**장점:**
- 스트림 처리와 저장이 하나의 플로우
- 즉시 저장 가능

**단점:**
- 스트림즈가 무거워짐
- DB 장애 시 스트림 전체 영향
- 권장되지 않는 패턴

```java
stream
    .foreach((key, value) -> {
        // 스트림즈 내에서 직접 DB 저장
        // 권장하지 않음!
        repository.save(entity);
    });
```

## 📁 이 프로젝트의 구조

### 1. Entity (엔티티)
```
entity/
├── HourlySalesResult.java      # 시간별 매출 집계 결과
├── DailySalesResult.java       # 일별 매출 집계 결과
└── EventCountResult.java       # 이벤트 카운트 결과
```

### 2. Repository (저장소)
```
repository/
├── HourlySalesResultRepository.java
├── DailySalesResultRepository.java
└── EventCountResultRepository.java
```

### 3. Service (비즈니스 로직)
```
service/
└── AggregationStorageService.java   # 메시지 파싱 & DB 저장
```

### 4. Consumer (카프카 리스너)
```
consumer/
└── TimeWindowConsumer.java          # Output 토픽 구독 & 저장 호출
```

## 🔄 데이터 흐름 상세

### 1단계: 카프카 스트림즈가 집계
```
hourly-sales-topic (입력)
  → 1시간 윈도우 집계
  → "상품:노트북, 시간대:2025-11-13 14:00~15:00, 총매출:5300000"
```

### 2단계: Output 토픽으로 전송
```
hourly-sales-output-topic (출력)
```

### 3단계: 컨슈머가 메시지 수신
```java
@KafkaListener(topics = "hourly-sales-output-topic")
public void listen(String message) {
    log.info("수신: {}", message);
    storageService.saveHourlySalesResult(message);
}
```

### 4단계: 서비스가 메시지 파싱 & DB 저장
```java
public void saveHourlySalesResult(String message) {
    // 1. 정규표현식으로 파싱
    Pattern pattern = Pattern.compile("상품:([^,]+), 시간대:([^~]+)~([^,]+), 총매출:(\\d+)");
    Matcher matcher = pattern.matcher(message);
    
    // 2. 엔티티 생성
    HourlySalesResult result = HourlySalesResult.builder()
        .productName("노트북")
        .windowStart(LocalDateTime.parse("2025-11-13 14:00"))
        .windowEnd(LocalDateTime.parse("2025-11-13 15:00"))
        .totalSales(5300000L)
        .build();
    
    // 3. DB 저장
    repository.save(result);
}
```

## 🗄️ 데이터베이스 구조

### hourly_sales_result (시간별 매출)
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | BIGINT | PK (자동증가) |
| product_name | VARCHAR | 상품명 |
| window_start | TIMESTAMP | 윈도우 시작 시간 |
| window_end | TIMESTAMP | 윈도우 종료 시간 |
| total_sales | BIGINT | 총 매출 |
| created_at | TIMESTAMP | 생성 시간 |

### daily_sales_result (일별 매출)
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | BIGINT | PK (자동증가) |
| category | VARCHAR | 카테고리 |
| sales_date | DATE | 판매 날짜 |
| total_sales | BIGINT | 총 매출 |
| created_at | TIMESTAMP | 생성 시간 |

### event_count_result (이벤트 카운트)
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| id | BIGINT | PK (자동증가) |
| event_type | VARCHAR | 이벤트 타입 |
| window_start | TIMESTAMP | 윈도우 시작 시간 |
| window_end | TIMESTAMP | 윈도우 종료 시간 |
| event_count | BIGINT | 발생 횟수 |
| created_at | TIMESTAMP | 생성 시간 |

## 🚀 사용 방법

### 1. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 2. 데이터 생성 (집계 결과가 자동으로 DB에 저장됨)
```bash
# 시간별 매출 데이터 생성
curl -X POST "http://localhost:8080/api/time-window/hourly-sales/bulk"

# 일별 매출 데이터 생성
curl -X POST "http://localhost:8080/api/time-window/daily-sales/bulk"

# 이벤트 데이터 생성
curl -X POST "http://localhost:8080/api/time-window/event/bulk"
```

### 3. 콘솔에서 DB 저장 확인
```
🕐 [시간별 집계 최종결과] 결과: 상품:노트북, 시간대:2025-11-13 14:00~15:00, 총매출:5300000
💾 [DB 저장 완료] 시간별 매출 - 상품: 노트북, 매출: 5300000원
```

### 4. API로 저장된 데이터 조회

#### Swagger UI 사용 (추천)
```
http://localhost:8080/swagger-ui.html
→ "집계 결과 조회 API" 섹션 확인
```

#### cURL 사용
```bash
# 최근 시간별 매출 조회
curl "http://localhost:8080/api/aggregation/hourly-sales/recent"

# 상품별 시간별 매출 조회
curl "http://localhost:8080/api/aggregation/hourly-sales/by-product?productName=노트북"

# 최근 일별 매출 조회
curl "http://localhost:8080/api/aggregation/daily-sales/recent"

# 카테고리별 일별 매출 조회
curl "http://localhost:8080/api/aggregation/daily-sales/by-category?category=전자제품"

# 최근 이벤트 카운트 조회
curl "http://localhost:8080/api/aggregation/event-count/recent"

# 전체 통계 조회
curl "http://localhost:8080/api/aggregation/stats"
```

### 5. H2 Console에서 직접 확인
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:kafkadb
User Name: sa
Password: (비워두기)
```

**SQL 쿼리 예시:**
```sql
-- 시간별 매출 전체 조회
SELECT * FROM hourly_sales_result ORDER BY created_at DESC;

-- 노트북 매출만 조회
SELECT * FROM hourly_sales_result WHERE product_name = '노트북';

-- 일별 매출 전체 조회
SELECT * FROM daily_sales_result ORDER BY sales_date DESC;

-- 이벤트 카운트 전체 조회
SELECT * FROM event_count_result ORDER BY created_at DESC;
```

## 💡 실무 팁

### 1. 메시지 포맷 설계
**현재 방식 (String):**
```
"상품:노트북, 시간대:2025-11-13 14:00~15:00, 총매출:5300000"
```

**권장 방식 (JSON):**
```json
{
  "productName": "노트북",
  "windowStart": "2025-11-13T14:00:00",
  "windowEnd": "2025-11-13T15:00:00",
  "totalSales": 5300000
}
```
- JSON이 파싱이 쉽고 확장성이 좋음
- JSON Serializer/Deserializer 사용

### 2. 에러 처리
```java
try {
    repository.save(entity);
} catch (Exception e) {
    log.error("DB 저장 실패", e);
    // Dead Letter Queue로 전송
    kafkaTemplate.send("dlq-topic", message);
}
```

### 3. 배치 저장으로 성능 개선
```java
@Scheduled(fixedDelay = 5000) // 5초마다
public void flushBatch() {
    repository.saveAll(batchBuffer);
    batchBuffer.clear();
}
```

### 4. 중복 저장 방지
```java
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_name", "window_start"})
})
public class HourlySalesResult {
    // 같은 상품의 같은 시간대는 한 번만 저장
}
```

### 5. 프로덕션 DB 사용
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
  jpa:
    hibernate:
      ddl-auto: validate  # 프로덕션에서는 validate 사용
```

## ⚖️ 방법 선택 가이드

### 커스텀 컨슈머 선택 시 ✅
- 비즈니스 로직이 필요할 때
- 메시지 변환/검증이 필요할 때
- 소규모~중규모 트래픽
- 빠른 개발이 필요할 때

### Kafka Connect 선택 시 ✅
- 대규모 데이터 처리
- 단순 토픽 → DB 연동
- 운영 인력이 충분할 때
- 고가용성이 필수일 때

## 🎯 결론

**Q: Output 토픽을 DB에 넣으려면?**

**A: 컨슈머에서 메시지 받아서 JPA로 저장!** ✅

```
Output Topic → @KafkaListener → Service → Repository → DB
```

이 방식이 가장 직관적이고 유연하며, 대부분의 실무 환경에서 사용됩니다!

