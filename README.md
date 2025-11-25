# Kafka Sample Project

## 프로젝트 구조

```
kafka-sample/
├── src/
│   ├── main/
│   │   ├── java/com/sample/kafka/
│   │   │   ├── KafkaSampleApplication.java       # 메인 애플리케이션
│   │   │   ├── config/
│   │   │   │   ├── KafkaTopicConfig.java      # 카프카 토픽 설정
│   │   │   │   ├── KafkaStreamsConfig.java    # 카프카 스트림즈 설정
│   │   │   │   └── SwaggerConfig.java         # Swagger 설정
│   │   │   ├── producer/
│   │   │   │   └── KafkaProducer.java         # 카프카 프로듀서
│   │   │   ├── consumer/
│   │   │   │   ├── KafkaConsumer.java         # 카프카 컨슈머
│   │   │   │   └── KafkaStreamsConsumer.java  # 스트림즈 결과 컨슈머
│   │   │   ├── controller/
│   │   │   │   ├── KafkaController.java       # REST API 컨트롤러
│   │   │   │   └── KafkaStreamsController.java # 스트림즈 API 컨트롤러
│   │   │   └── dto/
│   │   │       └── Message.java               # 메시지 DTO
│   │   └── resources/
│   │       └── application.yml                 # 애플리케이션 설정
│   └── test/
│       ├── java/com/sample/kafka/
│       │   ├── KafkaIntegrationTest.java           # 기본 통합 테스트
│       │   └── KafkaStreamsIntegrationTest.java    # 스트림즈 통합 테스트
│       └── resources/
│           └── application.yml                 # 테스트 설정
├── build.gradle                                # Gradle 빌드 설정
└── settings.gradle
```

## 주요 기능

### 1. 카프카 프로듀서 (KafkaProducer)
- 메시지를 카프카 토픽으로 전송
- 키가 있는 메시지와 키가 없는 메시지 모두 지원
- 비동기 전송 및 결과 로깅

### 2. 카프카 컨슈머 (KafkaConsumer)
- `@KafkaListener`를 사용한 메시지 수신
- 토픽, 파티션, 오프셋 정보 로깅

### 3. REST API (KafkaController)
- `POST /api/kafka/send` - 메시지 전송
- `POST /api/kafka/send-with-key` - 키와 함께 메시지 전송

### 4. 통합 테스트 (KafkaIntegrationTest)
- 임베디드 카프카를 사용한 테스트
- 프로듀서와 컨슈머 통합 테스트
- 단일 메시지 및 다중 메시지 테스트

### 5. 카프카 스트림즈 (Kafka Streams)
- **대문자 변환 스트림**: 메시지를 대문자로 변환
  - `input-topic` → 대문자 변환 → `output-topic`
- **필터링 스트림**: '중요' 키워드가 포함된 메시지만 통과
  - `filter-input-topic` → 필터링 → `filter-output-topic`
- **단어 카운트 스트림**: 실시간 단어 빈도 집계
  - `word-input-topic` → 단어별 카운트 → `word-count-output-topic`

### 6. 시간 윈도우 집계 (Time Window Aggregation) ⭐
- **시간별 집계 (1시간 윈도우)**: 상품별 시간당 매출 실시간 집계
  - `hourly-sales-topic` → 1시간 집계 → `hourly-sales-output-topic`
- **일별 집계 (1일 윈도우)**: 카테고리별 일 매출 실시간 집계
  - `daily-sales-topic` → 1일 집계 → `daily-sales-output-topic`
- **실시간 이벤트 카운팅 (5분 윈도우)**: 이벤트 타입별 발생 빈도 모니터링
  - `event-topic` → 5분 집계 → `event-count-output-topic`

> 💡 **실무 활용**: 실시간 대시보드, 매출 모니터링, 이상 탐지, 트래픽 분석 등에 널리 사용됩니다.
> 📖 자세한 내용은 [WINDOWED_AGGREGATION.md](WINDOWED_AGGREGATION.md) 참조

### 7. DB 저장 (Database Persistence) 💾
- **자동 저장**: Output 토픽의 집계 결과를 자동으로 DB에 저장
  - 컨슈머가 메시지 수신 → 파싱 → JPA로 저장
- **조회 API**: 저장된 집계 데이터를 조회하는 REST API 제공
- **H2 Console**: 브라우저에서 DB를 직접 확인 가능 (http://localhost:8080/h2-console)

> 💾 **저장 흐름**: `Output Topic` → `@KafkaListener` → `Service` → `Repository` → `DB`
> 📖 자세한 내용은 [DB_STORAGE_GUIDE.md](DB_STORAGE_GUIDE.md) 참조

### 8. Swagger UI (API 문서)
- SpringDoc OpenAPI를 사용한 API 문서화
- 인터랙티브한 API 테스트 환경
- 접속 URL: http://localhost:8080/swagger-ui.html

## 실행 방법

### 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션 실행 후 브라우저에서 **http://localhost:8080/swagger-ui.html** 접속하여 API 테스트 가능

### 테스트 실행

```bash
./gradlew test
```

## API 테스트

### Swagger UI 사용 (추천)

1. 애플리케이션 실행: `./gradlew bootRun`
2. 브라우저에서 접속: **http://localhost:8080/swagger-ui.html**
3. API 목록에서 원하는 엔드포인트 선택
4. "Try it out" 버튼 클릭
5. 파라미터 입력 후 "Execute" 버튼 클릭

### cURL 사용

#### 기본 카프카 API

```bash
# 메시지 전송
curl -X POST "http://localhost:8080/api/kafka/send?message=안녕하세요"

# 키와 함께 메시지 전송
curl -X POST "http://localhost:8080/api/kafka/send-with-key?key=user-123&message=사용자데이터"
```

#### 카프카 스트림즈 API

```bash
# 대문자 변환 스트림 테스트
curl -X POST "http://localhost:8080/api/kafka-streams/uppercase?message=hello world"

# 필터링 스트림 테스트 (중요 키워드 포함)
curl -X POST "http://localhost:8080/api/kafka-streams/filter?message=중요한 메시지입니다"

# 단어 카운트 스트림 테스트
curl -X POST "http://localhost:8080/api/kafka-streams/word-count?sentence=hello world hello kafka"

# 모든 스트림 동시 테스트
curl -X POST "http://localhost:8080/api/kafka-streams/test-all?message=중요한 Hello World"
```

#### 시간 윈도우 집계 API (실무 활용)

```bash
# 시간별 매출 대량 생성 (추천 - 한번에 여러 상품 매출 생성)
curl -X POST "http://localhost:8080/api/time-window/hourly-sales/bulk"

# 일별 매출 대량 생성
curl -X POST "http://localhost:8080/api/time-window/daily-sales/bulk"

# 실시간 이벤트 대량 생성
curl -X POST "http://localhost:8080/api/time-window/event/bulk"

# 개별 등록
curl -X POST "http://localhost:8080/api/time-window/hourly-sales?product=노트북&amount=1500000"
curl -X POST "http://localhost:8080/api/time-window/daily-sales?category=전자제품&amount=500000"
curl -X POST "http://localhost:8080/api/time-window/event?eventType=USER_LOGIN&data=user-123"
```

#### DB 조회 API

```bash
# 최근 시간별 매출 조회
curl "http://localhost:8080/api/aggregation/hourly-sales/recent"

# 상품별 시간별 매출 조회
curl "http://localhost:8080/api/aggregation/hourly-sales/by-product?productName=노트북"

# 최근 일별 매출 조회
curl "http://localhost:8080/api/aggregation/daily-sales/recent"

# 전체 통계 조회
curl "http://localhost:8080/api/aggregation/stats"
```

#### H2 Console (DB 직접 확인)

```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:kafkadb
User Name: sa
Password: (비워두기)
```

## 기술 스택

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Kafka**: Spring Boot 의존성 관리
- **Kafka Streams**: 실시간 스트림 처리
- **Spring Data JPA**: 데이터베이스 ORM
- **H2 Database**: 인메모리 데이터베이스 (테스트용)
- **SpringDoc OpenAPI**: 2.3.0 (Swagger UI)
- **Embedded Kafka**: 테스트용
- **Lombok**: 보일러플레이트 코드 감소
- **Gradle**: 빌드 도구

## 테스트 설명

### KafkaIntegrationTest (기본 카프카 테스트)

1. **testKafkaProducerAndConsumer**: 기본적인 프로듀서-컨슈머 테스트
2. **testKafkaProducerWithKey**: 키를 포함한 메시지 전송 테스트
3. **testMultipleMessages**: 여러 메시지 전송 및 수신 테스트

### KafkaStreamsIntegrationTest (스트림즈 테스트)

1. **testUpperCaseStream**: 대문자 변환 스트림 테스트
2. **testFilterStream**: 필터링 스트림 테스트 ('중요' 키워드 필터링)
3. **testWordCountStream**: 단어 카운트 스트림 테스트 (실시간 집계)

모든 테스트는 임베디드 카프카 브로커를 사용하여 실제 카프카 서버 없이 실행됩니다.

## 설정

### application.yml (main)
- 카프카 브로커: `localhost:9092`
- 컨슈머 그룹: `test-group`
- 자동 오프셋 리셋: `earliest`
- Swagger UI 경로: `/swagger-ui.html`
- API Docs 경로: `/api-docs`

### application.yml (test)
- 임베디드 카프카 브로커 사용
- 테스트 전용 설정

## 카프카 스트림즈 동작 방식

### 1. 대문자 변환 스트림
```
입력: "hello world" → 처리: 대문자 변환 → 출력: "HELLO WORLD"
```

### 2. 필터링 스트림
```
입력: "중요한 메시지" → 필터: '중요' 포함 확인 → 출력: "중요한 메시지" (통과)
입력: "일반 메시지" → 필터: '중요' 미포함 → 출력: (차단됨)
```

### 3. 단어 카운트 스트림 (상태 저장)
```
입력: "hello world hello"
출력:
  - hello: 1
  - world: 1
  - hello: 2 (누적)
```

## 실행 결과 확인

애플리케이션을 실행하고 API를 호출하면 콘솔에서 실시간 로그를 확인할 수 있습니다:

```bash
# 애플리케이션 실행
./gradlew bootRun

# 다른 터미널에서 API 호출
curl -X POST "http://localhost:8080/api/kafka-streams/uppercase?message=hello"
```

콘솔 출력 예시:
```
[스트림-대문자변환] 입력 - Key: null, Value: hello
[스트림-대문자변환] 변환 - hello -> HELLO
✅ [대문자변환 결과] Topic: output-topic, Offset: 0, Message: HELLO
```

## 🌟 시간 윈도우 집계 - 실무 핵심 기능

### 왜 실무에서 많이 사용하는가?

#### 1. 실시간 대시보드
```
배치 처리: 하루 후에 결과 확인 ❌
스트림즈: 초 단위로 실시간 업데이트 ✅
```

#### 2. 빠른 장애 감지
```
매출이 급감하거나 이상 트래픽 발생 시 즉시 알람
```

#### 3. 활용 사례
- **전자상거래**: 시간별/일별 매출 집계, 재고 모니터링
- **광고 플랫폼**: 실시간 노출수/클릭수 집계
- **게임**: 동시 접속자 수, 서버 부하 분석
- **금융**: 거래량 모니터링, 이상 거래 탐지
- **IoT**: 센서 데이터 집계, 이상치 탐지

### 테스트 예시

```bash
# 1. 애플리케이션 실행
./gradlew bootRun

# 2. 시간별 매출 데이터 생성
curl -X POST "http://localhost:8080/api/time-window/hourly-sales/bulk"
```

**콘솔에서 실시간 집계 결과 확인:**
```
[스트림-시간별집계] 상품: 노트북, 금액: 1500000, 누적: 0 -> 1500000
[스트림-시간별집계] 상품: 노트북, 금액: 1800000, 누적: 1500000 -> 3300000
✅ [시간별집계 결과] 상품:노트북, 시간대:2025-11-13 14:00~15:00, 총매출:5300000
```

📖 **더 자세한 내용**: 
- 시간 윈도우 집계: [WINDOWED_AGGREGATION.md](WINDOWED_AGGREGATION.md)
- 윈도우 타이밍 & DB 적재: [WINDOW_TIMING_GUIDE.md](WINDOW_TIMING_GUIDE.md)

## 참고사항

- 실제 운영 환경에서는 외부 카프카 클러스터를 사용하세요
- 테스트 환경에서는 `@EmbeddedKafka` 어노테이션으로 임베디드 카프카를 사용합니다
- 메시지 직렬화/역직렬화는 String 타입을 사용합니다 (커스터마이징 가능)
- 카프카 스트림즈는 상태 저장이 가능하여 실시간 집계 및 복잡한 스트림 처리가 가능합니다
- 스트림즈 애플리케이션은 카프카 컨슈머와 프로듀서를 모두 내장하고 있습니다
- **시간 윈도우 집계는 실무에서 매우 많이 사용되는 패턴입니다** (실시간 대시보드, 모니터링 등)

