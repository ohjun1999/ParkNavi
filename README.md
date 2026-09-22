# ParkNavi

시내버스 기사가 현재 위치 기준으로 **가까운 버스 정류장을 지도/목록으로 확인**할 수 있는 안드로이드 앱입니다.
운행 중 다음 정류장 위치를 빠르게 파악하는 것을 돕기 위한 내비게이션 보조 앱을 목표로 합니다.

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

## 스택

- Kotlin + Jetpack Compose (Material3)
- Hilt (DI)
- Retrofit + OkHttp + Gson
- Coroutines + StateFlow
- Navigation Compose
- Kakao Maps SDK v2 (지도)
- 국가대중교통정보센터(TAGO) 공공 버스정류소 API (좌표기준 근접 정류소 조회)
- 테스트: JUnit4 + MockK + Turbine + kotlinx-coroutines-test

## 실행 전 준비 (API 키 발급)

이 앱은 두 개의 외부 키가 필요합니다. `local.properties`(git에 커밋되지 않음)에 아래 두 값을 채워주세요.
`local.properties.example`을 복사해서 시작하면 편합니다.

```
TAGO_SERVICE_KEY=
KAKAO_NATIVE_APP_KEY=
```

1. **TAGO_SERVICE_KEY** — [공공데이터포털 "국토교통부_(TAGO)_버스정류소정보"](https://www.data.go.kr/data/15098534/openapi.do)에서
   활용신청 후 발급받은 **일반 인증키(Encoding)** 값을 그대로 붙여넣으세요. (디코딩 키를 넣으면 Retrofit이 다시 인코딩하면서
   이중 인코딩 오류가 날 수 있습니다 — [`TagoAuthInterceptor.kt`](app/src/main/java/com/jun/parknavi/data/remote/TagoAuthInterceptor.kt)가
   이미 인코딩된 키라고 가정하고 붙입니다.)
2. **KAKAO_NATIVE_APP_KEY** — [카카오 디벨로퍼스](https://developers.kakao.com/)에서 앱 생성 후 "네이티브 앱 키"를 발급받고,
   플랫폼 설정에 이 앱의 패키지명(`com.jun.parknavi`)과 키 해시를 등록하세요.

## 실행

```bash
./gradlew :app:assembleDebug
```

지도가 정상적으로 뜨려면 실제 기기/에뮬레이터에서 위치 권한을 허용해야 합니다(`StopMapScreen` 진입 시 권한을 요청합니다).

## 테스트

```bash
./gradlew :app:testDebugUnitTest
```

`BusStopRepositoryImplTest`(거리순 정렬, 실패 응답 처리), `ItemsDeserializerTest`(TAGO 응답의 item
배열/객체/빈 문자열 형태를 실제 Gson 파싱 경로로 검증), `NearbyStopsViewModelTest`(UiState 전이)가 포함되어 있습니다.

## 실기기/에뮬레이터로 검증하며 실제로 잡은 버그 2개

이 스켈레톤은 만들면서 끝낸 게 아니라, 에뮬레이터에 API 키를 넣고 직접 실행해서 검증했습니다. 그 과정에서
유닛 테스트만으로는 못 잡는 버그를 2개 발견해서 고쳤습니다.

1. **`ItemsDeserializer`가 "결과 0건" 응답을 못 받아냈다.** TAGO API는 결과가 없을 때 `items` 필드 자체가
   `{}`가 아니라 빈 문자열 `""`로 내려오는데, 처음 짠 디시리얼라이저는 이 경우를 처리하지 않아
   `IllegalStateException: Not a JSON Object: ""`로 죽었습니다. `BusStopRepositoryImplTest`는 API 응답을
   코틀린 객체로 직접 만들어서 Repository에 주입하는 방식이라 이 문제를 잡지 못했고, 실제 Gson 파싱을 거치는
   `ItemsDeserializerTest`를 추가하고 나서야 재발을 방지할 수 있었습니다.
2. **`LocationProvider`가 부정확한 위치를 줬다.** `getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, ...)`
   는 네트워크 기반 위치를 우선시해서, 에뮬레이터에서 `adb emu geo fix`로 넣은 좌표를 무시하고 구글 기본
   위치(마운틴뷰)를 반환했습니다. `PRIORITY_HIGH_ACCURACY`(GPS 우선)로 바꿔서 해결했는데, 이건 애초에
   "정확한 현재 위치 기준 근처 정류장"이 핵심 기능인 이 앱에는 배터리보다 정확도가 맞는 선택이기도 합니다.

## 알려진 한계 (다음 학습 주제)

- **TAGO API는 서울 커버리지가 거의 없습니다.** 서울은 TAGO에 "정적연계"로만 잡혀 있어 실질적으로 정류소
  데이터가 비어 있고(테스트 중 서울시청 좌표로 정상 응답 resultCode=00에 totalCount=0을 실제로 확인했습니다),
  서울은 별도의 TOPIS(서울시 교통정보시스템) API를 써야 합니다. 비수도권 좌표(대전 등)로 테스트하거나, 지역별로
  데이터 소스를 분기하는 건 다음 단계로 남겨뒀습니다.
- 위치/정류장 목록을 화면 단위(`hiltViewModel()`)로만 캐싱합니다 — 지도 화면 ↔ 목록 화면을 오갈 때마다 API를 다시 호출합니다.
  Repository 레벨 캐싱/단일 소스 전략은 다음 단계로 남겨뒀습니다.
- 위치 권한 거부 시 재요청 UX(설정으로 안내 등)는 구현하지 않았습니다.
- Kakao Maps SDK는 공식 Compose 래퍼가 없어 `AndroidView`로 감쌌습니다.
- 카카오 디벨로퍼스 계정에 이미 다른 앱이 카카오맵/로그인을 쓰고 있다면, 새 앱은 무료 쿼터 대상이 아니라
  비즈월렛(결제 카드) 연결이 필요할 수 있습니다 — 계정당 처음 활성화한 앱만 무료 쿼터를 받습니다.
