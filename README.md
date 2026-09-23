# ParkNavi

시내버스 기사가 현재 위치 기준으로 **가까운 버스 정류장을 지도/목록으로 확인**할 수 있는 안드로이드 앱입니다.
운행 중 다음 정류장 위치를 빠르게 파악하는 것을 돕기 위한 내비게이션 보조 앱을 목표로 하며, **서울 시내버스**를 대상으로 합니다.

이 저장소는 실제 서비스가 아니라, **Hilt(DI) + Repository 계층 + 테스트**를 MVVM 위에 올바르게
얹는 방법을 익히기 위한 아키텍처 스켈레톤입니다. 화면은 "내 주변 정류장 지도"와
"주변 정류장 목록" 두 개뿐이고, 나머지 기능(경로 안내, 로그인 등)은 의도적으로 비워뒀습니다.

## 아키텍처

```
Screen (Compose)  →  ViewModel (@HiltViewModel + StateFlow<UiState>)
                          →  Repository (interface + Impl)
                              →  ApiService (Retrofit) → DTO
```

레이어 경계와 컨벤션은 [`.claude/skills/parknavi-android-architecture-rule/SKILL.md`](.claude/skills/parknavi-android-architecture-rule/SKILL.md)에
자세히 정리되어 있습니다. Claude Code로 이 저장소를 열면 새 화면/기능을 추가할 때 이 규칙이 자동 적용됩니다.

지도 화면과 목록 화면은 nav-graph(`AppRoute.StopsGraph`)로 묶여서 `NearbyStopsViewModel` 인스턴스를
공유합니다 — 화면을 오가도 위치/정류장 데이터를 다시 조회하지 않습니다.

## 스택

- Kotlin + Jetpack Compose (Material3)
- Hilt (DI)
- Retrofit + OkHttp + Gson
- Coroutines + StateFlow
- Navigation Compose
- Kakao Maps SDK v2 (지도)
- 서울특별시_정류소정보조회 서비스(data.go.kr 경유, `ws.bus.go.kr`) — 좌표기반 근접 정류소 조회
- 테스트: JUnit4 + MockK + Turbine + kotlinx-coroutines-test

## 실행 전 준비 (API 키 발급)

이 앱은 두 개의 외부 키가 필요합니다. `local.properties`(git에 커밋되지 않음)에 아래 두 값을 채워주세요.
`local.properties.example`을 복사해서 시작하면 편합니다.

```
SEOUL_SERVICE_KEY=
KAKAO_NATIVE_APP_KEY=
```

1. **SEOUL_SERVICE_KEY** — [공공데이터포털 "서울특별시_정류소정보조회 서비스"](https://www.data.go.kr/data/3097890/openapi.do)에서
   **활용신청**(자동승인) 후 발급받은 **일반 인증키(Encoding)** 값을 그대로 붙여넣으세요. data.go.kr은 계정당 키 1개로
   활용신청한 모든 API를 쓸 수 있으므로, 이미 다른 data.go.kr API를 쓰고 있다면 같은 키로 이 상품에도 활용신청만
   하면 됩니다. (디코딩 키를 넣으면 Retrofit이 다시 인코딩하면서 이중 인코딩 오류가 날 수 있습니다 —
   [`SeoulAuthInterceptor.kt`](app/src/main/java/com/jun/parknavi/data/remote/SeoulAuthInterceptor.kt)가 이미 인코딩된
   키라고 가정하고 붙입니다.)

   > ⚠️ **주의**: 서울 열린데이터광장(data.seoul.go.kr)의 동일 이름 API들은 서비스 종료된 상태입니다. 반드시
   > **data.go.kr(공공데이터포털)**의 "서울특별시_정류소정보조회 서비스" 상품 페이지에서 활용신청하세요.
2. **KAKAO_NATIVE_APP_KEY** — [카카오 디벨로퍼스](https://developers.kakao.com/)에서 앱 생성 후 "네이티브 앱 키"를 발급받고,
   플랫폼 설정에 이 앱의 패키지명(`com.jun.parknavi`)과 키 해시를 등록하세요. 제품 설정에서 "카카오맵"을 활성화해야 하고,
   계정에 이미 다른 앱이 카카오맵/로그인을 쓰고 있다면 비즈월렛(결제 카드) 연결이 필요할 수 있습니다.

## 실행

```bash
./gradlew :app:assembleDebug
```

지도가 정상적으로 뜨려면 실제 기기/에뮬레이터에서 위치 권한을 허용해야 합니다(`StopMapScreen` 진입 시 권한을 요청합니다).
`ws.bus.go.kr`은 HTTPS를 지원하지 않아 해당 도메인만 [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml)에서
평문(cleartext) HTTP를 허용해뒀습니다.

## 테스트

```bash
./gradlew :app:testDebugUnitTest
```

`BusStopRepositoryImplTest`(거리순 정렬, 실패 응답 처리, 필드 누락 레코드 스킵),
`SeoulItemListDeserializerTest`(정류소 API 응답의 배열/단일 객체 형태를 실제 Gson 파싱 경로로 검증),
`NearbyStopsViewModelTest`(UiState 전이, 중복 조회 방지)가 포함되어 있습니다.

## 왜 TAGO가 아니라 서울시 API인가 (실제로 겪은 문제)

처음에는 국가대중교통정보센터(TAGO, 국토교통부) API로 짰다가 서울시 API로 바꿨습니다. 만들면서 끝낸 게
아니라 에뮬레이터에 실제 키를 넣고 검증하는 과정에서 이 문제를 실제로 발견했습니다.

**TAGO는 서울 커버리지가 거의 없습니다.** 서울은 TAGO에 "정적연계"로만 잡혀 있어 실질적으로 정류소 데이터가
비어 있습니다 — 서울시청 좌표로 조회해보면 정상 응답(`resultCode=00`)인데 결과가 0건으로 옵니다. 이 앱의
목적이 서울 시내버스라서, TAGO 연동 코드를 전부 걷어내고 **서울특별시_정류소정보조회 서비스**(`data.go.kr`
경유, 실제 호출 도메인은 `ws.bus.go.kr`)로 바꿨습니다. Repository 패턴 덕분에 이 교체가 `ViewModel`/`Screen`
레이어는 전혀 건드리지 않고 `data/remote`, `data/repository`, `di/NetworkModule.kt`만 바꿔서 끝났습니다 —
왜 ViewModel이 DTO나 특정 API 세부사항을 몰라야 하는지 보여주는 실제 사례입니다.

그 밖에 실기기 검증으로 잡은 버그:

- **`ItemsDeserializer`가 "결과 0건" 응답을 못 받아냈다.** (TAGO 시절) 결과가 없을 때 `items` 필드 자체가
  `{}`가 아니라 빈 문자열 `""`로 내려오는데, 처음 짠 디시리얼라이저는 이 경우를 처리하지 않아
  `IllegalStateException: Not a JSON Object: ""`로 죽었습니다. API 응답을 코틀린 객체로 직접 만들어서
  Repository에 주입하는 유닛 테스트는 이 문제를 잡지 못했고, 실제 Gson 파싱을 거치는 디시리얼라이저
  전용 테스트를 추가하고 나서야 재발을 방지할 수 있었습니다 — 서울 API로 바꾸면서도 같은 패턴
  (`SeoulItemListDeserializer` + `SeoulItemListDeserializerTest`)을 그대로 적용했습니다.
- **`LocationProvider`가 부정확한 위치를 줬다.** `getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, ...)`
  는 네트워크 기반 위치를 우선시해서, 에뮬레이터에서 `adb emu geo fix`로 넣은 좌표를 무시하고 구글 기본
  위치(마운틴뷰)를 반환했습니다. `PRIORITY_HIGH_ACCURACY`(GPS 우선)로 바꿔서 해결했는데, 이건 애초에
  "정확한 현재 위치 기준 근처 정류장"이 핵심 기능인 이 앱에는 배터리보다 정확도가 맞는 선택이기도 합니다.

## 알려진 한계 (다음 학습 주제)

- **서울시 API의 정확한 성공 코드(`headerCd`)와 응답 형태(JSON 기본 여부 등)는 아직 실기기로 완전히
  검증되지 않았습니다.** `data.go.kr` 상품 페이지의 문서를 기반으로 짰지만(`BusStopRepositoryImpl`의
  `RESULT_CODE_SUCCESS = "0"`에 TODO로 표시), 활용신청 승인 후 실제 응답으로 확인이 필요합니다.
- 위치 권한 거부 시 재요청 UX(설정으로 안내 등)는 구현하지 않았습니다.
- Kakao Maps SDK는 공식 Compose 래퍼가 없어 `AndroidView`로 감쌌습니다.
