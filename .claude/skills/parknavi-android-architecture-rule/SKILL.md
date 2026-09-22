---
name: parknavi-android-architecture-rule
description: ParkNavi Android 앱의 레이어 구조(MVVM + Repository + Hilt) 및 테스트 규약. 새 화면/기능을 추가하거나, 네트워크 API를 연동하거나, DI 모듈을 추가/수정할 때 적용한다.
---

# ParkNavi Android Architecture Rule

kingbusdriver의 `kingbus-android-mvvm-rule`을 참고해서 만든 규약이지만, 여기서는 한 걸음
더 나아가 **Hilt로 의존성을 주입**하고 **ViewModel과 ApiService 사이에 Repository를 둔다**.
kingbusdriver에는 아직 없는 두 가지 차이가 핵심이다.

## 레이어 (5단)

```
Screen (Composable)
  │  hiltViewModel()로 ViewModel을 받는다. UiState만 관찰.
  ▼
ViewModel (@HiltViewModel + StateFlow<UiState>)
  │  Repository/LocationProvider를 "생성자로 주입받는다" — 절대 직접 만들지 않는다.
  ▼
Repository (interface + Impl)
  │  DTO → 도메인 모델(data/model) 변환 지점. ApiService를 직접 감싼다.
  ▼
ApiService (Retrofit interface) → DTO (data/remote/dto)
```

**핵심 경계**
- **모든 의존성은 생성자 주입 + `@Inject`.** `= XxxApiService()` 같은 기본값 체인 금지(kingbusdriver의 흔적을 반복하지 않는다). "누가 이 객체를 만드는가"는 항상 `di/` 패키지의 `@Module` 하나로만 답할 수 있어야 한다.
- **ViewModel은 ApiService를 직접 모른다.** 반드시 Repository를 거친다. (kingbusdriver는 아직 이 경계가 없다 — ParkNavi에서는 지킨다.)
- **DTO는 Repository 밖으로 나가지 않는다.** ViewModel/Screen은 `data/model`의 도메인 모델(`BusStop`, `LatLng`)만 본다.

---

## 1. DTO / ApiService 레이어

**위치**: `data/remote/dto/`, `data/remote/*Api.kt`

- DTO는 서버(혹은 공공 API) 응답 필드를 그대로 옮긴 `data class`. 가공하지 않는다.
- 인증/공통 쿼리 파라미터(`serviceKey` 등)는 ApiService 시그니처에 넣지 않고 OkHttp `Interceptor`로 자동 주입한다. 예: [`TagoAuthInterceptor.kt`](../../app/src/main/java/com/jun/parknavi/data/remote/TagoAuthInterceptor.kt).
- 공공데이터포털류 API는 결과 0/1건일 때 배열 대신 객체가 오는 경우가 흔하다 — 이런 응답 형태 문제는 DTO를 늘리지 말고 `Gson` `JsonDeserializer`로 흡수한다. 예: [`ItemsDeserializer.kt`](../../app/src/main/java/com/jun/parknavi/data/remote/ItemsDeserializer.kt).

## 2. Repository 레이어

**위치**: `data/repository/{Entity}Repository.kt` (interface) + `{Entity}RepositoryImpl.kt`

```kotlin
interface BusStopRepository {
    suspend fun getNearbyStops(from: LatLng): List<BusStop>
}

class BusStopRepositoryImpl @Inject constructor(
    private val api: BusStopApi,
) : BusStopRepository {
    override suspend fun getNearbyStops(from: LatLng): List<BusStop> {
        val response = api.getNearbyStops(...)
        check(response.response.header.resultCode == "00") { response.response.header.resultMsg }
        return response.response.body.items?.item.orEmpty().map { it.toDomain(from) }
    }
}
```

- **인터페이스와 구현을 분리**하고 `di/RepositoryModule.kt`에서 `@Binds`로 연결한다. ViewModel/테스트는 인터페이스만 참조.
- 실패 판단(`resultCode` 체크 등 서버/외부 API 고유의 규칙)은 Repository의 책임. ViewModel은 `Throwable`만 다룬다.
- **안드로이드 프레임워크 클래스(`android.location.Location` 등)에 의존하지 않는다.** 순수 JVM 로직으로 짜면 Robolectric/instrumented test 없이도 일반 JUnit으로 바로 검증할 수 있다 (예: [`BusStopRepositoryImpl.distanceMeters`](../../app/src/main/java/com/jun/parknavi/data/repository/BusStopRepositoryImpl.kt)는 `Location.distanceBetween()` 대신 하버사인 공식을 직접 계산한다).

## 3. DI 모듈

**위치**: `di/NetworkModule.kt`, `di/RepositoryModule.kt`

- `@Provides`: 이 프로젝트가 만들지 않는 외부 타입(Retrofit, OkHttpClient, Gson)을 조립할 때.
- `@Binds`: 이 프로젝트가 만든 interface ↔ impl을 연결할 때 (`@Provides`보다 짧고, 컴파일 시점에 더 명확하다).
- `@Inject constructor`가 있는 클래스(`TagoAuthInterceptor`, `BusStopRepositoryImpl`, `LocationProvider`, `NearbyStopsViewModel` 등)는 별도 `@Provides` 없이 Hilt가 알아서 만든다 — 새 `@Module` 함수를 추가하기 전에 그냥 `@Inject constructor`로 되는지부터 확인한다.
- 새 API 도메인을 추가할 때는 `NetworkModule`에 함수를 더 늘리지 말고, 그 Api 인터페이스도 `@Inject constructor`를 쓸 수 있는 Repository 안에서 `retrofit.create(...)`로 직접 provide하는 한 줄만 추가한다.

## 4. ViewModel 규약

```kotlin
@HiltViewModel
class NearbyStopsViewModel @Inject constructor(
    private val repository: BusStopRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(val currentLocation: LatLng, val stops: List<BusStop>) : UiState()
        data class Failed(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching { /* repository/locationProvider 호출 */ }
                .onSuccess { _uiState.value = UiState.Loaded(...) }
                .onFailure { _uiState.value = UiState.Failed(it.message ?: "...") }
        }
    }
}
```

- `@HiltViewModel` + 생성자 `@Inject`. 기본값 있는 파라미터 금지(테스트에서 항상 명시적으로 넣는다).
- 로딩 상태는 `sealed class UiState`로 모델링 (kingbusdriver와 동일한 관례). API를 호출해서 화면을 그리는 모든 화면은 응답 전까지 `Loading`을 반드시 거친다.
- 중복 호출 방지: `Job`을 들고 있다가 `isActive`면 재진입을 막는다.
- ViewModel에 `import androidx.compose.*` 금지 — 순수 상태/로직만.

## 5. Screen(Composable) 규약

```kotlin
@Composable
fun StopListScreen(
    onBack: () -> Unit,
    viewModel: NearbyStopsViewModel = hiltViewModel(),   // viewModel() 아님, hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    when (val state = uiState) { ... }
}
```

- ViewModel 주입은 항상 `hiltViewModel()`. `viewModel()`(Hilt 없는 기본 팩토리)을 쓰면 `@Inject` 생성자를 못 찾는다.
- 화면 전환은 Screen의 콜백 파라미터(`onBack`, `onShowList` 등)로만 한다. ViewModel에 `NavController` 참조 금지 — `parknavi-android-navigation-rule`이 없어도 이 원칙은 `kingbus-android-mvvm-rule`과 동일하게 지킨다.
- 지도 SDK(KakaoMap) 같은 View 기반 라이브러리는 `AndroidView`로 감싸고, 그 안에서만 SDK 전용 타입(`com.kakao.vectormap.LatLng`)을 쓴다. ViewModel/Repository/도메인 모델에는 SDK 타입이 절대 나타나지 않는다 — 앱 자체의 `data/model/LatLng`만 오간다. 지도 SDK를 나중에 바꾸더라도 ViewModel 이하는 손대지 않아도 되게 하기 위함.

## 6. 테스트 규약

**위치**: `src/test/java/...` (JVM 단위 테스트만 — 이 프로젝트 범위에서는 instrumented test를 요구하지 않는다)

- **Repository 테스트**: `ApiService`를 MockK로 목킹하고, 성공/실패(결과 코드 이상) 두 경로를 검증한다. 예: [`BusStopRepositoryImplTest.kt`](../../app/src/test/java/com/jun/parknavi/data/repository/BusStopRepositoryImplTest.kt).
- **ViewModel 테스트**: Repository/LocationProvider를 MockK로 목킹하고, `StandardTestDispatcher` + `Dispatchers.setMain`/`resetMain` + Turbine의 `flow.test { }`로 `UiState` 전이를 검증한다. 예: [`NearbyStopsViewModelTest.kt`](../../app/src/test/java/com/jun/parknavi/ui/screen/stops/NearbyStopsViewModelTest.kt).
- 새 Repository/ViewModel을 추가하면, **같은 커밋에서** 최소 1개의 성공 케이스 + 1개의 실패 케이스 테스트를 함께 추가한다. "나중에 테스트 추가"는 허용하지 않는다.

## 금지 사항

- ViewModel/Screen에서 DTO(`*Dto`) 타입 직접 참조 금지.
- 기본값이 있는 생성자 파라미터로 의존성 제공 금지(`= XxxApiService()`) — 전부 `@Inject constructor` + Hilt 모듈.
- ViewModel/Repository에서 지도 SDK, `android.location.Location` 등 프레임워크·SDK 타입 직접 사용 금지 — 도메인 모델로 감싼다.
- `viewModel()` 사용 금지 — 항상 `hiltViewModel()`.
- 테스트 없는 Repository/ViewModel 신규 병합 금지.
