# 소비 트렌드 화면 구현 + 시안 디자인 적용

브랜치: `feature/spending-trends` → master
버전: 1.2.1.0 → 1.3.0.0 (기능 추가, minor)
설계 문서: `docs/20260612_spending-trends-design.md`

## 무엇을

용돈 기록이 리스트로만 쌓이던 것을, 카테고리 도넛·일별 추이·TOP5로 보이는 **통계 화면**을
추가하고, 앱 전체를 시안(Material You) 디자인으로 통일했다.

## 분류 룰 외부화 (핵심 관례)

파싱(`rules.json`)과 분류를 분리했다. 분류 규칙은 별도 파일 **`categories.json`**로 빼서
파싱 룰과 동일한 패턴(assets 기본값 → `Documents/Minseo5/`로 시드 → 외부 우선, 버전 기반
재시드)으로 관리한다. **바이너리(소스/APK) 수정 없이 파일만 고쳐** 분류를 바꾼다.

- `categories.json` 형식: `{ version, defaultCategory, categories: [{ category, keywords[] }] }`
- 분류는 **저장하지 않고 표시 시점에 계산**(`Categorizer`) → 룰을 고치면 과거 기록도 즉시 재분류
- DB 스키마/마이그레이션 변경 없음

## 추가/변경 파일

| 파일 | 역할 |
|---|---|
| `util/CategoryConfig.java` | 분류 룰 모델 |
| `util/CategoryStore.java` | categories.json 로더 (RuleStore와 동일 패턴) |
| `util/Categorizer.java` | 사용처 → 카테고리 (substring 매칭) |
| `util/CategoryColors.java` | 카테고리 → 파스텔 색 (리스트·도넛 공유) |
| `StatsActivity.java` + `activity_stats.xml` | 통계 화면 |
| `assets/categories.json` | 기본 분류 룰 (8개 카테고리) |
| `res/values/colors.xml`, `values-night/colors.xml` | 시안 팔레트 + 다크 변형 |
| `item_spending.xml`, `SpendingAdapter` | 리스트 행 시안화 (원형 타일) |
| `activity_main.xml`, `MainActivity` | 녹색 요약 카드, 메뉴/상태바 수정 |

## 의존성
- MPAndroidChart `com.github.PhilJay:MPAndroidChart:v3.1.0` (JitPack) — `settings.gradle.kts`에 JitPack 저장소 추가

## 버그 수정
- 메인 메뉴 미표시: `setSupportActionBar(toolbar)` 제거 (NoActionBar 테마 + Toolbar `app:menu` 충돌)
- 다크모드 차트 판독 불가: 차트 텍스트/축/범례를 테마 `colorOnSurface`로 resolve
- 상태바 아이콘 미가독: 배경 밝기에 따라 `setAppearanceLightStatusBars` 자동 설정

## 검증
- `./gradlew assembleDebug` BUILD SUCCESSFUL
- 플립(R3CX705W62D) 설치 후 메뉴→통계 진입, 다크모드 전환, 상태바 가독성 캡처 확인

## 후속
- (선택) 공유 카드 생성(Approach C): 통계를 이미지로 렌더해 ACTION_SEND
- categories.json 키워드 보강: 실제 사용처(data.json) 기준으로 "기타" 비중 낮추기
