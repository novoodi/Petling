# 골든 데이터셋 — 파싱 회귀 게이트

"OCR 텍스트 입력 → 기대 파싱 결과" 쌍을 자산으로 축적해, 파서를 수정할 때마다
정확도 변화를 자동 측정한다. 러너는 `GoldenDatasetTest`(JVM 유닛 테스트)이며
두 경로를 채점한다:

- **parser**: 규칙 파서 단독 (`KoreanScheduleParser`)
- **pipeline**: 분류 → 의도 정책 → 파싱 → 2-pass 재조정 (`GoldenPipeline`,
  `CaptureRepository.ingest()`의 파싱 파이프라인과 동일 조립)

Gemini Nano 경로는 기기 의존이라 CI 대상이 아니다. 데이터는 로컬 테스트
자산일 뿐 어디에도 전송되지 않는다.

## 구조

```
golden/
├── cases/            # 의도 라벨별 케이스 파일 (JSON 배열)
│   ├── chat.json     # 카톡 대화 (말풍선 타임스탬프 함정 포함)
│   ├── timetable.json
│   ├── shopping.json # 결제일·배송일·재고 비율 함정
│   ├── place.json    # 영업시간 함정
│   ├── link.json
│   ├── watermark.json# 촬영일 워터마크 (intent=memory)
│   └── plain.json    # 평문 메모 (파서 버그 재현 케이스 포함)
├── baseline.json     # 회귀 게이트 기준 수치
└── README.md
```

## 케이스 스키마

```json
{
  "id": "chat_001",              // 파일 접두사 + 일련번호, 전역 유일
  "intent": "chat",              // chat|timetable|shopping|place|link|memory|plain
  "text": "OCR 결과 시뮬레이션",  // 실제 캡처에서 왔다면 개인정보 제거·각색할 것
  "today": "2026-07-07",         // 상대 날짜("내일" 등) 해석 기준일 — 케이스마다 명시
  "expected": [                  // 기대 일정. 빈 배열 = "일정 없음"이 정답
    { "title": "치과", "date": "2026-07-08", "time": "15:00", "location": "치과" }
  ],
  "note": "케이스의 의도·함정 설명"
}
```

- `expected`는 **사람이 판단한 정답(ground truth)** 이다. 현재 파서가 맞히는 값이 아니다.
- `title`/`location`은 정규화(소문자·공백 제거) 후 포함 관계로 부분 채점되므로 핵심 키워드만 적는다.
- `time`이 null이면 종일 일정.

## 채점 방식 (GoldenScorer)

- confidence < 0.35이거나 date/time이 모두 없는 파싱 결과는 "추출"로 치지 않는다
  (weak 신호 캡과 맞물려 저신뢰 오탐을 흡수).
- 기대 항목과 추출을 그리디 매칭, 쌍 점수 = date 0.4 + time 0.3 + title 0.2 + location 0.1.
- 과잉 추출 감점: 기대 0건 케이스는 추출당 -0.5, 매칭 후 초과분은 건당 -0.25.
- 총점 = 케이스 평균. `baseline.json`의 `parserTotal`/`pipelineTotal`(총점)과
  `perCase`(케이스별 parser·pipeline 쌍) 대비 하락하면 테스트가 실패한다.
- 의도 분류 정확도는 리포트로만 출력한다(게이트 아님) — 분류 개선과 파싱
  개선을 분리 추적하기 위함.

## 케이스 추가 방법 (실사용 오파싱 발견 시)

1. 오파싱된 캡처의 OCR 텍스트를 확보하고 개인정보를 제거·각색한다.
2. 의도에 맞는 `cases/*.json` 파일에 케이스를 추가한다. `today`는 오파싱이
   재현되는 기준일로 고정하고, `expected`에는 사람이 판단한 정답을 적는다.
3. `gradlew :app:testDebugUnitTest --tests "*GoldenDatasetTest"` 실행.
   새 케이스가 낮은 점수여도 괜찮다 — **골든셋은 회귀 게이트이지 100점 게이트가 아니다.**
   현재 못 맞히는 케이스를 정직하게 담아야 개선이 수치로 드러난다.
4. 러너가 stdout에 출력한 새 baseline JSON을 `baseline.json`에 반영한다
   (`dataset_is_well_formed`의 케이스 수 assert도 함께 갱신).
5. 파서를 개선한 커밋에서는 전/후 `parserTotal`을 커밋 메시지에 기록한다.

`baseline.json`을 테스트가 자동으로 덮어쓰지 않는 것은 의도된 설계다 —
자동 갱신은 회귀 게이트를 무력화한다. 개선 의도가 있는 커밋에서만 수동 반영할 것.
