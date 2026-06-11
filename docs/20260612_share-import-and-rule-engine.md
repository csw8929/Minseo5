# 공유 기반 문자 가져오기 + 룰 엔진 외부화

브랜치: `fix/systembar-sms-list` → master
버전: 1.0.0.0 → 1.1.0.0

## 개요

용돈 기록 앱에 문자 기반 지출 입력을 추가하고, 파싱 규칙을 외부 파일로 분리했다.
초기에는 앱이 직접 SMS를 읽는 방식이었으나, 권한 부담과 UX를 고려해 **공유(Share) 기반**으로 전환했다.

## 작업 내역

### 1. 시스템 바 가림 수정
- Target SDK 35+의 강제 엣지투엣지로 toolbar/FAB이 status·navigation bar와 겹치던 문제 수정
- `WindowManager.getCurrentWindowMetrics().getWindowInsets()`로 top/bottom 인셋을 구해 toolbar padding, FAB translationY에 반영
- `setOnApplyWindowInsetsListener` 콜백이 일부 단말에서 호출되지 않아 직접 메트릭을 읽는 방식으로 처리

### 2. 문자 가져오기 — 공유 방식으로 전환
- Android에는 "문자 앱에서 메시지를 골라 결과로 돌려받는" 표준 인텐트가 없음
- 대신 `MainActivity`에 `ACTION_SEND`(text/plain) intent-filter를 등록
- "문자에서 가져오기" → 기본 문자 앱(`CATEGORY_APP_MESSAGING`) 실행 → 사용자가 메시지 공유 → 앱으로 전달
- `launchMode="singleTop"` + 다이얼로그/액티비티를 resume 이후 표시 (singleTask는 삼성 단말에서 새 task 생성 후 파괴되는 문제 있었음)
- READ_SMS 권한 완전 제거

### 3. 파싱 화면 (SmsPickerActivity)
- 공유로 들어온 텍스트 표시 (직접 붙여넣기도 가능)
- "파싱 시작" → 룰 적용 결과를 목록(RecyclerView)으로 표시
- "적용" → 결과 전부를 DB에 일괄 insert
- 한 문자에 여러 건(`\n[Web발신]\n` 반복)이 있어도 find-all로 모두 추출
- 적용 후 가장 최근 거래 월로 MainActivity 화면 자동 이동 (ActivityResult로 날짜 전달)

### 4. 룰 엔진 외부화
- 목표: 소스 수정 없이 파싱 규칙만 바꿀 수 있게
- `RuleConfig` / `RuleStore` / `SmsParser.parseAll()` 구조
- 룰 파일: `Documents/Minseo5/rules.json` (정규식 + 캡처그룹 매핑)
- 앱 시작 시 외부 파일을 읽고, 없으면 `assets/rules.json`(기본 룰)을 복사해 시드
- 권한 없으면 번들 기본 룰로 동작 (graceful fallback)
- 연도 없는 MM/DD 날짜는 `recentPast` 휴리스틱으로 추론 (오늘보다 미래면 작년)

기본 룰 (하나 체크카드 승인):
```
하나[\d*]+\s*(?:체크승인)?\s*차\*서\s*([\d,]+)원\s*(\d{1,2})/(\d{1,2})\s+(\d{1,2}):(\d{2})\s+(.+)
```
캡처: 1=금액, 2=월, 3=일, 6=상호(용도)

### 5. data.json 저장
- 공유로 가져온 원문(raw)을 `Documents/Minseo5/data.json`에 문자열 배열로 누적
- `DataJsonStore`: Android 10+는 MediaStore, 구버전은 직접 파일

### 6. 권한
- `MANAGE_EXTERNAL_STORAGE`("모든 파일 액세스"): 외부 rules.json 읽기/시드, data.json 접근
- 첫 실행 시 안내 다이얼로그 → 설정으로 유도. 미허용 시 기본 룰로만 동작

## 주요 버그 수정
- 입력 다이얼로그 진입 시 `ClassCastException`: `MaterialAlertDialogBuilder.create()`는 `androidx.appcompat.app.AlertDialog`를 반환하는데 import가 `android.app.AlertDialog`였음
- "적용했는데 리스트에 안 보임": 거래 날짜(과거 월)로 저장됐는데 화면은 현재 월을 보고 있던 것 → 적용 후 거래 월로 자동 이동으로 해결

## 개인정보 주의
- `tools/data.json`, `docs/20260611_sms_01055232287.md`는 실제 가족 문자 데이터라 `.gitignore`로 제외 (커밋 금지)
