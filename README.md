# oh-my-photogrid

## 다운로드
**[최신 APK 받기 (Releases)](https://github.com/Canine89/oh-my-photogrid/releases/latest)** — `oh-my-photogrid-1.5.0.apk`

설치 방법 (Galaxy Z Fold 8 / Android 10 이상):
1. APK를 휴대폰에 받은 뒤 **내 파일**에서 연다. **"이 출처 허용"**을 켜고 **설치**를 누른다
2. 설치가 막히면 **설정 → 보안 및 개인정보 보호 → 자동 차단**을 잠시 끄고 다시 시도한다
3. 또는 PC에서 `adb install -r oh-my-photogrid-1.5.0.apk`

무결성 확인: `shasum -a 256 oh-my-photogrid-1.5.0.apk`의 결과를 릴리스에 첨부된 `.sha256` 파일과 비교한다.

**업데이트**
- 1.4.0부터는 새 버전이 나오면 홈 화면에 알림이 뜬다. **업데이트**를 누르면 앱이 APK를 받아 시스템 업데이트 창을 띄운다. 처음 한 번만 "이 출처 허용"을 켜면 된다.
- 1.3.0 이하를 쓰고 있다면 1.4.0은 직접 설치해야 한다. 기존 앱을 지우지 말고 새 APK를 그대로 설치하면 **업데이트**로 설치되고, 설정과 저장한 사진이 그대로 남는다. 모든 버전이 같은 패키지명(`app.wireframephoto`)과 같은 서명 인증서를 쓰기 때문이다.


갤럭시 Z 폴드 8 전용 사진 콜라주 앱. 커버 화면(10:16)에서는 한 손으로 빠르게, 메인 화면(4:3)에서는 2단 구성으로 넓게 편집한다.
폴드 화면 해상도에 딱 맞는 배경화면과 SNS용 콜라주를 저장한다. 광고와 워터마크가 없다. 인터넷은 새 버전 확인과 업데이트 다운로드에만 쓴다(GitHub에 요청 1회, 최대 6시간마다).

- 첫 화면에서 "무엇을 만들까요?"를 묻는다. 커버/메인 배경화면, 인스타 피드, 스토리 등을 카드로 고르면 바로 사진 선택으로 넘어간다
- 사진 1~9장, 템플릿 48종. 캔버스 비율에 맞는 레이아웃을 자동으로 골라 준다
- 편집 도구는 배치 · 사진 · 꾸미기 · 크기 4개. 처음 들어가면 사용법 안내가 뜬다
- **디자인**: 두 가지 테마. 홈 화면에서 전환한다
  - 다크룸: 사진이 주인공, 라임 강조색
  - 가족 앨범: 말랑한 젤리 질감, 꿀·딸기 젤리 버튼, 둥근 Jua 서체, 누르면 꿀렁이는 모션
  - 공통: Pretendard 서체, 스프링 모션, 떠 있는 알약형 도구 막대, 사진 색으로 번지는 캔버스 뒤 빛, 저장할 때 인화되듯 나타나는 완성본
- **섞어 보기**(주사위): 같은 사진으로 배치·스타일·배경을 무작위로 바꿈. 되돌리기 가능
- 비율 프리셋: 폴드 8 메인/커버, 폴드 8 Ultra 메인/커버, 1:1, 4:5, 3:4, 9:16, 16:9
- 칸 안에서 핀치로 확대, 드래그로 이동, 두 번 탭해 맞춤. 길게 눌러 끌면 다른 칸과 자리를 바꾼다
- 간격, 여백, 모서리 둥글기, 배경색 조절. 실행 취소/다시 실행
- 미리보기와 같은 구도로 고해상도 JPEG/PNG를 `Pictures/oh-my-photogrid`에 저장 → 공유 / 배경화면으로 설정
- 폴더블 대응: 접었을 때는 하단 도구 막대, 펼쳤을 때는 오른쪽 탭 패널, 테이블탑에서는 접힌 선 기준 위아래 분할. 접거나 펴도 편집 상태 유지
- 갤러리에서 공유로 받기, 분할 화면에서 드래그 앤 드롭으로 사진 넣기

## 문서
- [docs/01-product-plan.md](docs/01-product-plan.md) — 기획서
- [docs/02-dev-plan.md](docs/02-dev-plan.md) — 개발 계획, 아키텍처, 테스트 전략
- [docs/03-research-notes.md](docs/03-research-notes.md) — 리서치 노트 (기기 사양, 경쟁 앱, 플랫폼 제약, 사이드로딩 정책)
- [docs/04-release.md](docs/04-release.md) — 빌드, 테스트, 서명, 배포, Fold 8 에뮬레이터, 실기기 체크리스트
- [docs/05-design-system.md](docs/05-design-system.md) — 디자인 리서치와 Darkroom 디자인 시스템

## 빠른 시작
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21 ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :app:testDebugUnitTest          # 단위 테스트
./gradlew :app:connectedDebugAndroidTest  # 에뮬레이터 계측 테스트
scripts/build_release.sh                  # 서명된 APK → dist/
scripts/create_fold8_avd.sh               # Galaxy Z Fold 8 규격 에뮬레이터 생성
```
