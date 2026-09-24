# 빌드·테스트·배포 가이드

## 1. 개발 환경 (이 Mac에 설치된 구성)
| 항목 | 경로/버전 |
|---|---|
| JDK | `brew install openjdk@21` → `/opt/homebrew/opt/openjdk@21` |
| Android SDK | `brew install --cask android-commandlinetools` → `/opt/homebrew/share/android-commandlinetools` |
| SDK 패키지 | platforms;android-37.0, build-tools;37.0.0, platform-tools, emulator, system-images;android-37.0;google_apis;arm64-v8a |
| 에뮬레이터 | **AVD `Galaxy_Z_Fold_8`**: Fold 8 해상도를 복제한 폴더블 에뮬레이터(`scripts/create_fold8_avd.sh`로 생성). 이 밖에 AVD `fold`(Pixel 9 Pro Fold)가 있음 |

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
```

## 2. 테스트
```bash
# JVM 단위 테스트 (기하·크롭·템플릿·상태·섞기) — 35개
./gradlew :app:testDebugUnitTest

# Galaxy Z Fold 8 에뮬레이터 (처음 한 번 생성하면, 이후에는 -avd Galaxy_Z_Fold_8로 실행)
scripts/create_fold8_avd.sh
$ANDROID_HOME/emulator/emulator -avd Galaxy_Z_Fold_8 -no-audio -gpu host &
#  - 펼쳤을 때 2448×1848(4:3), 접었을 때 1248×1972(10:16), 420dpi
#  - 에뮬레이터의 접기 기능은 구글 폴더블 패널 규격에서만 동작한다. 그래서 그 규격으로 부팅하고
#    패널마다 wm size로 해상도를 덮어쓴다(덮어쓴 값은 유지됨)
#  - One UI가 아니라 순정 Android다. 앱 레이아웃과 해상도 검증용

# 반쯤 접기(테이블탑)는 posture 2, 완전히 펼치기는 3
adb emu posture 2
adb emu fold / adb emu unfold # 접기/펴기 (접으면 화면이 꺼지므로 adb shell input keyevent KEYCODE_WAKEUP)

# 계측 테스트 (렌더러 픽셀 검증, EXIF 회전, MediaStore 저장, 홈·에디터 UI, 안내, 섞기, 화면 재생성) — 16개
# 접힌 상태와 펼친 상태에서 각각 실행한다(접힌 화면 전용 테스트 1개는 펼친 상태에서 건너뜀)
./gradlew :app:connectedDebugAndroidTest && scripts/results.py
```

로컬 UI 스모크 테스트 도구:
- `scripts/demo_flow.sh N "목적 카드 제목"`: 앱을 실행해 목적 카드를 누르고 사진 N장을 고른다
- `scripts/ui.py`: uiautomator로 탭·덤프
- `scripts/shot.sh 이름`: 지금 켜져 있는 화면(커버 또는 메인)을 `test-output/`에 캡처
- `scripts/ui.py tap-scroll "글자"`: 스크롤하면서 찾아서 탭

## 3. 릴리스 서명 키
- `keystore/release.jks`와 `keystore.properties`(비밀번호)는 로컬에서 생성했고 `.gitignore`에 넣어 두었다.
- **두 파일을 반드시 안전한 곳(비밀번호 관리자, 암호화된 백업)에 백업한다.** 키를 잃어버리면 이미 설치된 앱을 업데이트할 수 없다(삭제 후 다시 설치해야 함).
- 인증서 SHA-256: `2b:44:85:05:44:38:cf:b2:71:5e:a7:4b:af:7f:bb:a8:bf:94:5c:0d:ff:72:e3:e0:30:36:81:99:71:50:16:6f`
  (2027년 Google 개발자 인증에 등록할 때 이 값을 쓴다)
- 서명 방식: v2 + v3 (v3이 있어서 나중에 서명 키를 교체할 수 있다)

## 4. 릴리스 빌드
```bash
scripts/build_release.sh
# → dist/oh-my-photogrid-<version>.apk 와 .sha256 생성
```
배포할 때마다 `app/build.gradle.kts`의 `versionCode`를 1씩 올리고, `versionName`도 바꾼다.

### 앱 안 업데이트 (1.4.0부터)
- 앱은 화면에 돌아올 때마다(최소 10분 간격) `https://api.github.com/repos/Canine89/oh-my-photogrid/releases/latest`를 확인한다(`data/UpdateChecker.kt`)
  - 홈 화면 맨 아래의 **업데이트 확인**을 누르면 간격과 상관없이 바로 확인한다. 전에 닫은 버전도 다시 보여 준다
  - 1.4.0~1.6.1은 앱을 새로 켤 때만, 6시간 간격으로 확인했다. 그래서 새 버전이 늦게 뜰 수 있다
  - 태그 `vX.Y.Z`가 설치된 `versionName`보다 높으면 홈 화면에 알림을 띄운다
- **업데이트**를 누르면 릴리스에서 `.apk`로 끝나는 첫 번째 파일을 받는다. 그다음 `PackageInstaller`로 시스템 업데이트 창을 띄운다(`data/UpdateInstaller.kt`)
  - "이 출처 허용"이 꺼져 있으면 먼저 설정 화면으로 보낸다. 사용자가 허용하고 돌아오면 자동으로 이어서 진행한다
- 따라서 릴리스를 만들 때 지킬 것:
  - 태그는 반드시 `vX.Y.Z` 형식으로 붙인다
  - APK를 릴리스 파일로 첨부한다
  - Draft나 Pre-release로 만들지 않는다. `latest`에 잡히지 않아서 알림이 가지 않는다
- 서명 키가 다르면 Android가 업데이트를 거부한다. 그래서 다른 사람이 만든 APK가 이 경로로 설치될 수 없다

## 5. 기기에 설치
1. **ADB (권장, 개발자 인증 예외 대상)**
   폴드 8에서 개발자 옵션 → USB 디버깅을 켠 뒤:
   ```bash
   adb install -r dist/oh-my-photogrid-1.7.0.apk
   ```
2. **파일로 전달**
   APK를 기기로 옮긴 뒤 내 파일 앱에서 열기 → "이 출처 허용" → 설치.
   Galaxy 기기의 "자동 차단(Auto Blocker)"이 켜져 있으면 설치가 막힌다. 설정 → 보안 및 개인정보 보호에서 잠시 끈다.

## 6. Google 개발자 인증 일정 (2026-09-24 기준)
- 2026-09-30: 브라질, 인도네시아, 싱가포르, 태국에서 먼저 시행. **한국은 아직 대상이 아니다.**
- 2027년: 전 세계로 확대 예정. 그 전에 [Android Developer Console](https://developer.android.com/developer-verification)에서
  패키지명 `app.wireframephoto`와 위 인증서를 등록한다.
  - 무료 "취미/학생" 계정: 기기 20대까지
  - 정식 계정: 25달러, 신분 확인 필요
- `adb install`은 인증 대상에서 제외된다.

## 7. 실기기(Galaxy Z Fold 8) 확인 체크리스트
에뮬레이터로 대신할 수 없는 항목:
- [ ] 커버 화면에서 시작 → 펼치기: 편집 상태 유지, 2단 구성으로 전환
- [ ] 메인 화면에서 편집 중 접기: 커버 화면에서 이어서 편집 가능
- [ ] Flex 모드(반쯤 접어 세우기): 캔버스가 접힌 선 위에 오는지
- [ ] 분할 화면에서 Samsung 갤러리 사진을 캔버스 칸으로 드래그 앤 드롭
- [ ] 갤러리 → 공유 → oh-my-photogrid로 여러 장 보내기
- [ ] 50MP 원본(및 Ultra의 200MP 원본)과 HEIC 사진 저장
- [ ] 저장 후 "배경화면으로 설정" → 커버/메인 배경화면이 잘림 없이 딱 맞는지
- [ ] 다크 모드, 시스템 글꼴 크기를 가장 크게 했을 때
