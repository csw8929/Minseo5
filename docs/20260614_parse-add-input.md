# 파싱 화면 "추가 입력" + 금액 색상

브랜치: `feature/parse-add-input` → master
버전: 1.3.0.0 → 1.4.0.0 (기능 추가, minor)

## 변경

### 추가 입력 (여러 문자 모아 파싱)
- "파싱 시작" 옆에 "추가 입력" 버튼. **공유로 진입한 경우(EXTRA_FROM_SHARE)만** 표시 (클립보드 가져오기엔 숨김)
- 누르면 현재 입력 내용을 `SmsPickerActivity.pendingPrepend`에 보관하고 문자앱을 연 뒤 finish
- 다음 공유가 `MainActivity.handleSendIntent`로 들어오면 `pendingPrepend + "\n\n" + 새 문자`로 합쳐 다시 파싱 화면을 띄움 → 원하는 만큼 반복

### data.json 저장 위치 이동
- 저장 책임을 SmsPickerActivity → MainActivity로 이동. 공유/클립보드로 **들어온 문자만** 1번 저장하므로, 추가 입력으로 합쳐 다시 열어도 앞부분이 중복 저장되지 않음

### 금액 색상
- 중복 제외 최종 금액: 빨강 굵게 (`tv_unique_total`, `expense_red`)
- 항목 금액: 파란색 굵게 (`amount_blue` — 라이트 `#1565C0`, 다크 `#64B5F6`) — 파싱 결과 + 메인 리스트 공통

## 파일
- `SmsPickerActivity.java`, `MainActivity.java`, `activity_sms_picker.xml`, `item_parse_result.xml`, `item_spending.xml`, `colors.xml`, `values-night/colors.xml`

## 검증
- `./gradlew assembleDebug` BUILD SUCCESSFUL
- 플립(R3CX705W62D) 설치 확인
