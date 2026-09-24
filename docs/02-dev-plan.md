# 개발 계획서

## 1. 기술 스택
- Kotlin 2.4.20 · Jetpack Compose (BOM 2026.09.00) · Material 3
- material3-adaptive 1.3.0: WindowSizeClass, 테이블탑 자세 감지
- AGP 9.4.1 (내장 Kotlin) · Gradle 9.7 · JDK 21
- minSdk 29 / compileSdk·targetSdk 37
- 외부 이미지 라이브러리 없이 `ImageDecoder`와 `LruCache`만 쓴다(의존성과 APK 크기 최소화). 인터넷은 GitHub 릴리스 확인과 업데이트 다운로드에만 쓴다(1.4.0부터).

## 2. 아키텍처 (단일 모듈, MVVM)

```
app/src/main/java/app/wireframephoto/
├─ core/            순수 Kotlin 로직 — JVM 단위 테스트 대상
│  ├─ Geometry.kt       NRect/PxRect, CellTransform, CollageStyle, 셀 사각형 계산(간격·여백)
│  ├─ CropMath.kt       center-crop + 확대/이동 계산, 제스처 적용, 디코딩 크기 계산
│  ├─ Templates.kt      1~9장 템플릿 정의(정규화 좌표)
│  ├─ AspectPresets.kt  폴드8 / 폴드8 Ultra / SNS 비율 프리셋
│  └─ CollageState.kt   편집 상태(Parcelable)와 상태 변경 함수(사진 교체, 템플릿 변경 시 사진 재배치 등)
├─ render/
│  ├─ BitmapLoader.kt   ImageDecoder 기반 디코딩 + 미리보기용 LRU 캐시
│  └─ CollageRenderer.kt 오프스크린 Canvas로 고해상도 렌더링
├─ data/
│  └─ ImageSaver.kt     MediaStore 저장, 공유/배경화면 Intent
└─ ui/
   ├─ MainActivity.kt   Photo Picker, 드래그 앤 드롭 권한 처리
   ├─ EditorViewModel.kt 상태·실행 취소 스택·저장 작업
   ├─ home/HomeScreen.kt
   └─ editor/           EditorScreen(적응형 레이아웃), CollageCanvas(제스처), ToolPanels
```

핵심 설계 원칙: **미리보기와 저장이 같은 순수 함수(CollageGeometry, CropMath)를 쓴다.**
확대/이동 값은 해상도와 무관한 정규화 좌표로 저장한다. 그래서 화면에서 본 구도가 4096px 결과물에서도 똑같이 나온다(WYSIWYG).

## 3. 적응형 레이아웃 규칙
| 조건 | 배치 |
|---|---|
| 테이블탑 (반쯤 접음 + 가로 접힌 선) | 위: 캔버스 / 아래: 도구 (접힌 선 위치에서 나눔) |
| 폭 ≥ 600dp이고 가로가 세로보다 김 (메인 화면 기본 상태) | 왼쪽 캔버스(가중치 1.6) + 오른쪽 도구 패널(360~420dp) |
| 그 외 (커버 화면, 메인 화면 세로) | 위 캔버스 + 아래 탭 도구 |

## 4. 단계별 일정 (마일스톤)
| 단계 | 내용 | 완료 기준 |
|---|---|---|
| M0 | 환경: JDK 21, SDK 37, 에뮬레이터, Gradle 래퍼 | `./gradlew assembleDebug` 성공 |
| M1 | core 로직 + 단위 테스트 | 기하·크롭·템플릿 테스트 통과 |
| M2 | 렌더러와 저장 | 계측 테스트에서 결과 이미지 크기와 픽셀 색 검증 |
| M3 | 에디터 UI, 제스처, 실행 취소 | 에뮬레이터에서 수동 확인 + 스크린샷 |
| M4 | 폴더블 대응: 적응형 레이아웃, 상태 유지, 드래그 앤 드롭 | 커버/메인 해상도 에뮬레이션 스크린샷, 회전·크기 변경 후 상태 유지 |
| M5 | release 서명 APK, R8 축소, 설치 테스트 | `adb install` 후 실행, 핵심 흐름 스모크 테스트 |

## 5. 테스트 전략
1. **JVM 단위 테스트** (`./gradlew testDebugUnitTest`)
   - 셀 사각형: 간격이 정확히 g px인지, 여백, 셀이 캔버스를 벗어나지 않는지
   - 모든 템플릿: 셀이 [0,1] 범위 안에 있는지, 겹치지 않는지, 면적 합이 1인지(빈틈 없이 채우는지)
   - CropMath: 기본 center-crop 결과, 확대 시 clamp, 제스처 후 초점 고정, 디코딩 크기 상한
   - 상태 변경: 사진 교체, 템플릿 변경 시 사진 유지, 실행 취소
2. **계측 테스트** (`./gradlew connectedDebugAndroidTest`, 에뮬레이터)
   - 합성 이미지(단색 비트맵, EXIF 회전 JPEG)를 렌더링해 결과 크기와 셀 중심 픽셀 색 확인
   - MediaStore 저장 후 다시 읽어서 크기 확인
   - Compose UI 테스트: 에디터 화면 표시, 템플릿 선택
3. **폴드 시뮬레이션** (`adb shell wm size` / `wm density`)
   - 커버 1248×1972, 메인 2448×1848(가로), 메인을 세로로 돌린 1848×2448에서 스크린샷 비교
   - 크기를 바꾼 뒤(접기/펴기와 같은 효과) 편집 상태가 유지되는지 확인
4. **release APK 검증**: `apksigner verify --print-certs`, R8 적용 빌드로 설치해 실행

## 6. 배포
- `keystore/release.jks`는 로컬에서 생성하고 `.gitignore`에 넣는다. 비밀번호는 `keystore.properties`에 둔다(커밋 금지).
- 산출물: `app/build/outputs/apk/release/app-release.apk`. 버전 규칙: versionCode 정수 증가, versionName은 SemVer.
- 설치: `adb install -r app-release.apk`, 또는 파일로 전달해 "출처를 알 수 없는 앱" 설치를 허용.
- 2027년 Google 개발자 인증 전면 시행 전에 Android Developer Console에 패키지명(`app.wireframephoto`)과 서명 인증서를 등록한다.

## 7. 리스크와 대응
| 리스크 | 대응 |
|---|---|
| 실기기(Fold 8) 없이 검증 | 해상도·밀도 에뮬레이션 + 리사이즈 가능한 에뮬레이터. 실기기 체크리스트는 README에 제공 |
| 200MP 원본 때문에 OOM | 디코딩 픽셀 수 상한 + 셀 크기에 맞춘 targetSize + 한 장씩 순서대로 렌더링 |
| Photo Picker URI 권한이 사라짐 | `takePersistableUriPermission` 시도, 실패하면 셀을 빈 칸으로 표시 |
| 드래그 앤 드롭 URI 권한 | `requestDragAndDropPermissions`로 권한을 받은 뒤 즉시 미리보기를 디코딩하고 캐시 |
