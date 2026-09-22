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

`BusStopRepositoryImplTest`(거리순 정렬, 실패 응답 처리), `NearbyStopsViewModelTest`(UiState 전이)가 포함되어 있습니다.

## 알려진 한계 (다음 학습 주제)

- 위치/정류장 목록을 화면 단위(`hiltViewModel()`)로만 캐싱합니다 — 지도 화면 ↔ 목록 화면을 오갈 때마다 API를 다시 호출합니다.
  Repository 레벨 캐싱/단일 소스 전략은 다음 단계로 남겨뒀습니다.
- 위치 권한 거부 시 재요청 UX(설정으로 안내 등)는 구현하지 않았습니다.
- Kakao Maps SDK는 공식 Compose 래퍼가 없어 `AndroidView`로 감쌌습니다 — SDK 특성상 실제 기기에서만
  마커 렌더링을 눈으로 확인할 수 있습니다.
