# 중복 체크 + 선택 합계 + 미파싱 표시 + README

브랜치: `feature/add-new-feature` → master
버전: 1.1.1.0 → 1.2.0.0

## 작업 내역

### 1. 하단 navigation bar 겹침 수정
- 루트 `CoordinatorLayout` → `LinearLayout` + `FrameLayout`(list+FAB)로 교체
- scrolling-behavior 자식이 bottom 패딩을 무시해 리스트가 nav bar 밑까지 그려지던 문제 해결
- `root_layout`에 systemBars 인셋 패딩, RecyclerView paddingBottom=88dp(FAB 가림 방지)

### 2. 시각 저장 + 중복 체크
- `SpendingRecord.usedTime` 컬럼 추가 (Room v2, 개발 중이라 destructive 마이그레이션)
- 룰에 `hourGroup/minGroup` 추가, `SmsParser`가 시·분 캡처
- 중복 키 = 날짜 + 시각 + 사용처 + 금액 (`SpendingDao.countDup`)
- 파싱 화면: DB·배치 중복을 **연한 빨강(#FFCDD2) 배경**으로 표시, 적용 시 제외
- rules.json 버전 기반 재시드(v1→v2): 외부 파일이 구버전이면 assets 기본 룰로 갱신

### 3. 합계 표시
- 파싱 시작 시 하단에 `합계` / `중복 제외(적용 대상)` 금액 표시

### 4. 파싱 안 된 항목 표시
- `[Web발신]` 블록 중 룰에 안 잡힌 것을 **파란색(#1565C0) 리스트**로 표시
- 금액 유무와 무관하게 인식 실패 블록 전부 표시

### 5. 메인 리스트 선택 모드 + 합계
- tap = 수정(기존), **long tap = 선택 모드** 진입
- 선택 모드: tap 토글(연한 파랑 배경), 툴바 "N개 선택" + **합계** 버튼, ←/뒤로가기로 종료
- 합계 → 선택 항목 목록 + 합계 금액 다이얼로그 (`dialog_sum`, `selection_menu`)

### 6. 복사된 항목 가져오기 (앞선 커밋에서 병합)
- 클립보드 텍스트를 공유와 동일 경로로 파싱 화면에 전달

### 7. README + 배포 등록
- 아주 쉬운 사용법 README(화면 도식) 작성
- APK 출력명 `Minseo5.apk` 지정, `scripts/apk.sh` 래퍼로 MinseoStore 배포 등록

## 주요 수정
- 같은 문자 반복 가져오기 시 중복 저장되던 문제 → 시각 기반 중복 키로 해결
- AlertDialog 캐스팅 크래시(이전 브랜치에서 수정)와 별개로, 선택/합계 다이얼로그는 androidx AlertDialog 사용
