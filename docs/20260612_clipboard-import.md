# 복사된 항목 가져오기

브랜치: `feat/clipboard-import` → master
버전: 1.1.0.0 → 1.1.1.0

## 변경
- FAB "추가 방법 선택" 다이얼로그에 세 번째 항목 **"복사된 항목 가져오기"** 추가
- 클립보드(`ClipboardManager`)의 텍스트를 읽어 `SmsPickerActivity`로 전달
- 이후 동작은 문자 공유와 완전히 동일: 원문 data.json 저장 → 파싱 시작 → 적용(일괄 DB) → 적용 후 거래 월 이동
- 기존 `smsPickerLauncher`를 재사용해 결과 처리(월 이동) 일관성 유지
- 클립보드가 비었거나 텍스트가 아니면 "복사된 텍스트가 없습니다" 토스트

## 파일
- `MainActivity.java`: `showAddOptions()` 항목 추가, `importFromClipboard()` 신규
