# Minseo5 초기 구현

## 앱 개요

용돈 기록 앱. 월별 지출 내역 관리, SMS 파싱으로 자동 입력, JSON 내보내기/가져오기 지원.

## 구현 내용

### 빌드 환경 픽스
- Gradle 9.3.1 → 8.13 다운그레이드 (foojay 플러그인 호환성 문제)
- AGP 8.13.2로 변경
- `settings.gradle.kts` foojay 플러그인 블록 제거
- `gradle.properties`에 `android.useAndroidX=true` 추가

### 파일 구조

```
db/
  SpendingRecord.java   — Room 엔티티 (usedDate, amount, purpose, entryDate)
  SpendingDao.java      — 월별 조회, 합계, 중복 체크, CRUD
  SpendingDatabase.java — 싱글톤, allowMainThreadQueries

util/
  SmsParser.java        — 정규식 기반 SMS 파싱 (금액, 용도 추출)
  JsonExportImport.java — SAF 기반 Gson JSON I/O

ui/
  SpendingAdapter.java  — 지출 목록 RecyclerView
  SmsAdapter.java       — SMS 목록 RecyclerView
  SpendingEntryDialog.java — DialogFragment (추가/수정/삭제), onStart() 유효성 검사

MainActivity.java       — 메인 화면 (월 탐색, FAB, 메뉴)
SmsPickerActivity.java  — SMS 인박스 목록 → 파싱 → 입력 다이얼로그
```

### 주요 설계 결정

- **SMS 파싱**: best-effort 정규식. 금액(`(\d[\d,]*)원`), 이체(`에서|로부터`), 카드(`승인`), Web발신 패턴. 파싱 결과를 사용자가 검토 후 수정 가능.
- **중복 방지**: usedDate + amount + purpose 3-field 매칭. 가져오기 시 적용.
- **다이얼로그**: MaterialAlertDialogBuilder + `setPositiveButton(null)` + onStart()에서 버튼 리스너 override → 유효성 검사 후 dismiss 제어.

## 확인 리스트

1. 앱 이름 "용돈 기록" 표시 여부
2. 이번 달 목록이 처음에 표시되는지
3. 이전/다음달 네비게이션 동작
4. FAB → "문자에서 선택" / "직접 입력" 다이얼로그 표시
5. 직접 입력: 날짜/금액/용도 입력 후 저장, 목록에 반영
6. 문자에서 선택: SMS 권한 요청 → SMS 목록 → 항목 클릭 → 파싱된 값 입력 다이얼로그
7. 항목 클릭 → 수정/삭제 가능
8. 메뉴 → 내보내기/가져오기 (JSON 파일)
