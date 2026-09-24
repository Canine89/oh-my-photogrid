# 리서치 노트 (2026-09-24 기준)

## 1. 대상 기기: Galaxy Z Fold 8 / Fold 8 Ultra
- 2026-07-22 Unpacked에서 발표, 2026-08-07 출시. Android 17 / One UI 9 탑재
- **Fold 8**: 메인 7.6" 2448×1848 (4:3, 가로가 기본), 커버 5.5" 1248×1972 (10:16). 펼친 크기 161.4×123.9mm, "여권형"으로 넓어짐. 201g
- **Fold 8 Ultra**: 메인 8.0" 2504×2256, 커버 6.5" 1080×2520 (21:9). Fold 7의 후속 모델
- S Pen은 두 모델 모두 미지원
- 출처: [Wikipedia](https://en.wikipedia.org/wiki/Samsung_Galaxy_Z_Fold_8), [Engadget](https://www.engadget.com/2220490/samsung-galaxy-z-fold-8-launch-date-price-new-design/), [Samsung US](https://www.samsung.com/us/smartphones/galaxy-z-fold8/)

## 2. 경쟁 앱
| 앱 | 장점 | 불만/한계 |
|---|---|---|
| Layout (Instagram) | 최대 9장, 드래그로 교체, 핀치 확대, 단순함 | 2024-05 서비스 종료 |
| PicCollage | 쉬움, 평점 높음 | 구독 결제 전에는 저장 불가, 워터마크, 광고 |
| Canva | 템플릿이 매우 많음 | 모바일에서는 화면이 비좁고, 좋은 기능은 Pro 전용 |
| Samsung 갤러리 | 기본 탑재, 테두리·둥글기 조절 | 최대 6장, 비율 1:1·9:16·16:9뿐 |
| Google 포토 | 교체·재정렬·공유 편리 | 최대 6장 |

→ 이 앱의 차별점: **9장까지 · 폴드 화면 비율 프리셋 · 광고/워터마크/구독 없음 · 원본에 가까운 해상도로 저장 · 폴더블 전용 UI**
→ Instagram 피드 기본 비율이 3:4(1080×1440)로 바뀌었으므로 3:4 프리셋을 넣는다.

## 3. 플랫폼 제약
- **Android 16 (API 36)**: 최소 폭(sw)이 600dp 이상인 화면에서는 `screenOrientation`, `resizableActivity`, 최소/최대 화면 비율 설정이 무시된다.
  **Android 17 (API 37)**은 이를 피하던 opt-out 속성까지 없앴다. 따라서 메인 화면에서는 모든 크기와 방향에 대응해야 한다.
- 폴드를 접거나 펴면 Activity가 다시 만들어질 수 있다. 편집 상태는 ViewModel과 SavedStateHandle에 둔다.
- 테이블탑(Samsung Flex 모드)은 `FoldingFeature`가 HALF_OPENED 상태이고 접힌 선이 HORIZONTAL일 때다.
  `currentWindowAdaptiveInfo().windowPosture.isTabletop`으로 감지한다.
- 다른 앱에서 드래그 앤 드롭을 받을 때는 `Modifier.dragAndDropTarget`과 `requestDragAndDropPermissions`를 함께 쓴다.
- Photo Picker는 권한 없이 쓸 수 있다. 최대 선택 수는 `MediaStore.getPickImagesMaxLimit()`를 넘지 않게 한다.
- MediaStore 저장은 API 29 이상에서 권한이 필요 없다(`RELATIVE_PATH`와 `IS_PENDING` 사용).

## 4. 고해상도 저장
- `GraphicsLayer.toImageBitmap()`은 화면 해상도로만 캡처되므로 쓰지 않는다. 오프스크린 `Bitmap` + `Canvas`로 따로 렌더링한다.
- 원본 사진은 셀 크기에 맞춰 줄여서 디코딩한다(`ImageDecoder.setTargetSize`). ImageDecoder는 EXIF 회전을 자동으로 적용한다.
- 결과 비트맵은 100MB 이하로 유지하고(4096×4096 ARGB = 64MB), 화면에는 절대 그리지 않는다.
- 소프트웨어 Canvas에 그릴 소스 비트맵은 `ALLOCATOR_SOFTWARE`로 디코딩한다(하드웨어 비트맵은 그릴 수 없음).

## 5. APK 사이드로딩 배포
- v2+v3 서명(apksigner 기본값). **키스토어를 잃어버리면 같은 앱으로 업데이트할 수 없으므로 반드시 백업한다.**
- Google 개발자 인증(미등록 앱 설치 차단)
  - 2026-09-30부터 브라질, 인도네시아, 싱가포르, 태국에서 먼저 시행. **한국은 아직 대상이 아니다.**
  - 2027년에 전 세계로 확대 예정. 그 전에 Android Developer Console에 패키지명과 서명 인증서를 등록해야 한다.
    - 무료 "취미/학생" 계정: 기기 20대까지
    - 정식 계정: 25달러, 신분 확인 필요
  - `adb install`은 인증 없이 설치할 수 있다(예외).
- 출처: [developer verification](https://developer.android.com/developer-verification), [FAQ](https://developer.android.com/developer-verification/guides/faq)

## 6. 확인한 최신 버전 (Google Maven / Maven Central, 2026-09-24)
| 구성요소 | 버전 |
|---|---|
| Android Gradle Plugin | 9.4.1 (Gradle 9.6+ / JDK 17+) |
| Kotlin | 2.4.20 (AGP 9 내장 Kotlin 사용) |
| Compose BOM | 2026.09.00 |
| material3-adaptive | 1.3.0 |
| androidx.window | 1.5.1 |
| activity-compose | 1.13.0 |
| lifecycle | 2.11.0 |
| compileSdk / targetSdk | 37 (Android 17) |
