# Suppress - 중간 결과 억제하기

## 🎯 문제 상황

### 사용자 질문
**"지금 5분짜리 집계를 여러 번 나눠서 하고 DB 저장도 여러 번 되고 있는데?"**

## ❌ 문제: suppress() 없을 때

```java
// 현재 코드 (문제)
.count()
.toStream()  // ← 업데이트될 때마다 전송!
.to("output-topic");
```

### 실제 동작

```
5분 윈도우 (14:00~14:05)

14:00:10 이벤트 1개 도착
  → count: 1
  → Output 토픽 전송 ✅
  → 컨슈머 수신
  → DB 저장 (id: 1, count: 1)

14:00:30 이벤트 1개 도착
  → count: 2
  → Output 토픽 전송 ✅
  → 컨슈머 수신
  → DB 저장 (id: 2, count: 2)  ← 중복!

14:01:00 이벤트 1개 도착
  → count: 3
  → Output 토픽 전송 ✅
  → 컨슈머 수신
  → DB 저장 (id: 3, count: 3)  ← 중복!

14:02:00 이벤트 1개 도착
  → count: 4
  → Output 토픽 전송 ✅
  → 컨슈머 수신
  → DB 저장 (id: 4, count: 4)  ← 중복!

14:03:00 이벤트 1개 도착
  → count: 5
  → Output 토픽 전송 ✅
  → 컨슈머 수신
  → DB 저장 (id: 5, count: 5)  ← 중복!

결과: 같은 윈도우(14:00~14:05)에 대해 DB에 5개 레코드! ❌
```

### DB 상태
```sql
SELECT * FROM event_count_result 
WHERE event_type = 'USER_LOGIN' 
AND window_start = '2025-11-13 14:00:00';

id | event_type  | window_start        | window_end          | event_count | created_at
---|-------------|---------------------|---------------------|-------------|--------------------
1  | USER_LOGIN  | 2025-11-13 14:00:00 | 2025-11-13 14:05:00 | 1           | 2025-11-13 14:00:10
2  | USER_LOGIN  | 2025-11-13 14:00:00 | 2025-11-13 14:05:00 | 2           | 2025-11-13 14:00:30
3  | USER_LOGIN  | 2025-11-13 14:00:00 | 2025-11-13 14:05:00 | 3           | 2025-11-13 14:01:00
4  | USER_LOGIN  | 2025-11-13 14:00:00 | 2025-11-13 14:05:00 | 4           | 2025-11-13 14:02:00
5  | USER_LOGIN  | 2025-11-13 14:00:00 | 2025-11-13 14:05:00 | 5           | 2025-11-13 14:03:00
   ↑ 같은 윈도우인데 5번 저장됨!
```

## ✅ 해결: suppress() 추가

```java
// 수정된 코드
.count()
.suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))  // ← 추가!
.toStream()
.to("output-topic");
```

### 수정 후 동작

```
5분 윈도우 (14:00~14:05)

14:00:10 이벤트 1개 도착
  → count: 1
  → Output 토픽 전송 안 함 ❌ (suppress!)

14:00:30 이벤트 1개 도착
  → count: 2
  → Output 토픽 전송 안 함 ❌ (suppress!)

14:01:00 이벤트 1개 도착
  → count: 3
  → Output 토픽 전송 안 함 ❌ (suppress!)

14:02:00 이벤트 1개 도착
  → count: 4
  → Output 토픽 전송 안 함 ❌ (suppress!)

14:03:00 이벤트 1개 도착
  → count: 5
  → Output 토픽 전송 안 함 ❌ (suppress!)

14:05:01 새 윈도우 데이터 도착
  → 14:00~14:05 윈도우 닫힘!
  → count: 5
  → Output 토픽 전송 ✅ (이때만!)
  → 컨슈머 수신
  → DB 저장 (id: 1, count: 5)  ← 딱 1번만!

결과: 같은 윈도우(14:00~14:05)에 대해 DB에 1개 레코드! ✅
```

### DB 상태 (수정 후)
```sql
SELECT * FROM event_count_result 
WHERE event_type = 'USER_LOGIN' 
AND window_start = '2025-11-13 14:00:00';

id | event_type  | window_start        | window_end          | event_count | created_at
---|-------------|---------------------|---------------------|-------------|--------------------
1  | USER_LOGIN  | 2025-11-13 14:00:00 | 2025-11-13 14:05:00 | 5           | 2025-11-13 14:05:01
   ↑ 딱 1번만 저장됨! (최종 결과)
```

## 🔍 suppress() 상세 설명

### 문법

```java
.suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
```

### 각 부분 설명

#### 1. Suppressed.untilWindowCloses()
```
"윈도우가 완전히 닫힐 때까지 억제(suppress)한다"
```

#### 2. BufferConfig.unbounded()
```
"버퍼 크기 제한 없음"
= 윈도우가 닫힐 때까지 모든 중간 결과를 메모리에 보관
```

#### 3. 다른 옵션들

**시간 기반:**
```java
.suppress(Suppressed.untilTimeLimit(
    Duration.ofSeconds(30),  // 30초마다 전송
    Suppressed.BufferConfig.maxRecords(1000)  // 최대 1000개
))
```

**레코드 수 기반:**
```java
.suppress(Suppressed.untilWindowCloses(
    Suppressed.BufferConfig.maxRecords(100)  // 최대 100개만 버퍼링
))
```

## 📊 비교표

| 항목 | suppress() 없음 | suppress() 있음 |
|------|----------------|----------------|
| **전송 시점** | 업데이트될 때마다 | 윈도우 닫힐 때만 |
| **전송 횟수** | 많음 (N번) | 적음 (1번) |
| **DB 레코드** | 중복 다수 | 최종 결과만 |
| **네트워크 부하** | 높음 ⚠️ | 낮음 ✅ |
| **메모리 사용** | 낮음 | 약간 높음 |
| **실시간성** | 매우 높음 | 약간 낮음 |
| **정확도** | 중간 결과 | 최종 결과 ✅ |

## 🎯 언제 어떤 것을 사용?

### suppress() 사용 (추천) ✅

**상황:**
- 최종 집계 결과만 필요
- DB에 한 번만 저장하고 싶을 때
- 네트워크 트래픽 절약
- 정확한 윈도우 집계

**예시:**
- 시간별/일별 매출 집계
- 주기적인 리포트
- 대시보드 (정각 업데이트)

```java
.count()
.suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
.toStream()
.to("output-topic");
```

### suppress() 없음 (실시간 모니터링)

**상황:**
- 실시간 업데이트가 중요
- 중간 경과도 봐야 할 때
- 즉각적인 알림 필요

**예시:**
- 실시간 트래픽 모니터링
- 이상 탐지 (급증/급감)
- 라이브 대시보드

```java
.count()
.toStream()
.to("output-topic");

// DB에서는 UPSERT 처리
// 같은 윈도우면 UPDATE, 없으면 INSERT
```

## 🔧 DB 중복 방지 방법

### 방법 1: suppress() 사용 (권장) ✅

```java
// 스트림즈 설정
.suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
```

### 방법 2: DB Unique 제약조건

```java
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_type", "window_start", "window_end"})
})
public class EventCountResult {
    // 같은 윈도우는 한 번만 저장
}
```

### 방법 3: UPSERT (ON CONFLICT)

```java
@Transactional
public void saveOrUpdate(EventCountResult result) {
    // 같은 윈도우가 있으면 UPDATE, 없으면 INSERT
    eventCountRepository.findByEventTypeAndWindowStart(
        result.getEventType(), 
        result.getWindowStart()
    ).ifPresentOrElse(
        existing -> {
            existing.setEventCount(result.getEventCount());
            eventCountRepository.save(existing);
        },
        () -> eventCountRepository.save(result)
    );
}
```

### 방법 4: 최신 것만 유지

```java
@Transactional
public void save(EventCountResult result) {
    // 같은 윈도우의 이전 레코드 삭제
    eventCountRepository.deleteByEventTypeAndWindowStart(
        result.getEventType(),
        result.getWindowStart()
    );
    
    // 새로 저장
    eventCountRepository.save(result);
}
```

## 🚀 실제 테스트

### 수정 전 (suppress 없음)

```bash
# 이벤트 5개 전송
curl -X POST "http://localhost:8080/api/time-window/event/bulk"

# DB 확인
curl "http://localhost:8080/api/aggregation/event-count/recent"

# 결과: 여러 개 레코드
[
  {"id": 1, "eventCount": 1, ...},
  {"id": 2, "eventCount": 2, ...},
  {"id": 3, "eventCount": 3, ...},
  {"id": 4, "eventCount": 4, ...},
  {"id": 5, "eventCount": 5, ...}
]
```

### 수정 후 (suppress 추가)

```bash
# 이벤트 5개 전송
curl -X POST "http://localhost:8080/api/time-window/event/bulk"

# 5분 후 또는 새 윈도우 데이터 전송
# ...

# DB 확인
curl "http://localhost:8080/api/aggregation/event-count/recent"

# 결과: 딱 1개 레코드 (최종 결과)
[
  {"id": 1, "eventCount": 5, ...}
]
```

## 📝 정리

### 문제
- **suppress() 없이** 윈도우 집계 시
- 중간 결과가 계속 Output 토픽으로 전송됨
- DB에 같은 윈도우 데이터가 여러 번 저장됨

### 해결
```java
.suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
```

### 효과
- ✅ 윈도우 닫힐 때만 1번 전송
- ✅ DB에 최종 결과만 저장
- ✅ 네트워크 트래픽 감소
- ✅ 정확한 집계 결과

### 주의
- 실시간 업데이트가 필요하면 suppress() 사용 안 함
- 대신 DB에서 UPSERT 처리

이제 문제가 명확하게 해결될 것입니다! 🎉

