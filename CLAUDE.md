# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Minseo5 (`com.example.minseo5`) — Android 앱 프로젝트. 현재 빈 스캐폴드 상태로, 아직 앱 기능이 구현되지 않았습니다.

## Build

```bash
# 디버그 빌드
./gradlew assembleDebug

# 빌드 후 설치 (단말 지정)
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

## Stack

- **언어**: Java 11
- **Min SDK**: 24 / **Target SDK**: 36
- **AGP**: 8.13.2 / **Gradle**: 8.13 (Kotlin DSL)
- **의존성**: AppCompat, Material, Activity, ConstraintLayout

## 워크스페이스 관례

이 프로젝트는 `D:\workspace` 아래의 Android 앱 중 하나입니다. 워크스페이스 공통 관례(`D:\workspace\CLAUDE.md`)를 따릅니다.

### VERSION 파일

루트의 `VERSION` 파일(`1.0.0.0`)을 `app/build.gradle.kts`가 자동으로 읽어 `versionCode`/`versionName`을 계산합니다.

### 단말 시리얼

공통 단말 매핑은 `D:\workspace\CLAUDE.md` 참조 (`R54Y1003KXN` 탭, `R3CT70FY0ZP` 폴드 등).

## Skill routing

When the user's request matches an available skill, invoke it via the Skill tool. When in doubt, invoke the skill.

Key routing rules:
- Product ideas/brainstorming → invoke /office-hours
- Strategy/scope → invoke /plan-ceo-review
- Architecture → invoke /plan-eng-review
- Design system/plan review → invoke /design-consultation or /plan-design-review
- Full review pipeline → invoke /autoplan
- Bugs/errors → invoke /investigate
- QA/testing site behavior → invoke /qa or /qa-only
- Code review/diff check → invoke /review
- Visual polish → invoke /design-review
- Ship/deploy/PR → invoke /ship or /land-and-deploy
- Save progress → invoke /context-save
- Resume context → invoke /context-restore
