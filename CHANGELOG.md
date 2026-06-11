# Changelog

이 파일은 Minseo5(용돈 기록) 앱의 주요 변경 사항을 기록합니다.

## [1.1.0.0] - 2026-06-12

### Added
- 문자 앱에서 공유로 지출 내역 가져오기: 문자 앱을 열어 메시지를 공유하면 앱으로 텍스트가 전달됨 (READ_SMS 권한 불필요)
- 파싱 화면: 가져온 문자 표시 → "파싱 시작"으로 결과(날짜/금액/용도) 확인 → "적용"으로 일괄 DB 저장
- 한 문자에 여러 건이 들어있어도 모두 추출 (find-all)
- 외부 룰 파일 `Documents/Minseo5/rules.json`: 정규식 기반 파싱 규칙을 소스 수정 없이 편집. 앱 시작 시 읽고, 없으면 기본 룰(assets) 자동 생성
- 가져온 문자 원문을 `Documents/Minseo5/data.json`에 누적 저장
- 적용 후 해당 거래 월로 화면 자동 이동

### Changed
- 상단 status bar / 하단 navigation bar를 가리지 않도록 시스템 인셋 처리
- SMS 목록 UI를 카드형으로 정리 (이후 공유 방식 전환으로 목록 화면은 파싱 화면으로 대체)

### Fixed
- 입력 다이얼로그 진입 시 `AlertDialog` 캐스팅 크래시 (`android.app.AlertDialog` → `androidx.appcompat.app.AlertDialog`)
- 연도 없는 카드 문자(MM/DD)의 날짜를 가장 최근 과거 기준으로 추론

## [1.0.0.0] - 2026-06-11

### Added
- 용돈 기록 앱 초기 구현: 월별 지출 목록, 이전/다음달 이동, 직접 입력, JSON 내보내기/가져오기, Room DB 저장
