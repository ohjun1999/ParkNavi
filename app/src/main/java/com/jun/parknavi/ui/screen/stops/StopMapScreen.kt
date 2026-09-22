package com.jun.parknavi.ui.screen.stops

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng as AppLatLng
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles

private const val TAG = "StopMapScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopMapScreen(
    onShowList: () -> Unit,
    viewModel: NearbyStopsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapErrorMessage by remember { mutableStateOf<String?>(null) }
    // 직전에 그린 (위치, 정류장 목록)을 기억해서, 같은 데이터로 uiState/kakaoMap이 다시
    // 바뀌었을 뿐일 때(예: 화면 재진입) 마커를 지웠다가 다시 그리는 걸 건너뛴다.
    var drawnStops by remember { mutableStateOf<List<BusStop>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 권한을 거부해도 load()는 호출한다 — ViewModel의 hasLocationPermission() 체크가
        // "위치 권한이 필요합니다" Failed 상태를 만들어준다. 여기서 granted만 보고 막아버리면
        // 거부했을 때 로딩 스피너가 영원히 멈춰서, 그 실패 경로 자체가 도달 불가능해진다.
        viewModel.load()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(uiState, kakaoMap) {
        val state = uiState
        val map = kakaoMap
        if (state is NearbyStopsViewModel.UiState.Loaded && map != null && state.stops != drawnStops) {
            map.drawStops(state.currentLocation, state.stops)
            drawnStops = state.stops
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("내 주변 정류장") },
                actions = {
                    FloatingActionButton(onClick = onShowList) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "목록 보기")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            KakaoMapView(
                modifier = Modifier.fillMaxSize(),
                onMapReady = { kakaoMap = it; mapErrorMessage = null },
                onMapDestroyed = { kakaoMap = null },
                onMapError = { error ->
                    Log.e(TAG, "카카오맵 초기화 실패", error)
                    kakaoMap = null
                    mapErrorMessage = "지도를 불러오지 못했습니다."
                },
            )
            when {
                mapErrorMessage != null ->
                    Text(mapErrorMessage.orEmpty(), modifier = Modifier.align(Alignment.Center))
                uiState is NearbyStopsViewModel.UiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState is NearbyStopsViewModel.UiState.Failed ->
                    Text(
                        (uiState as NearbyStopsViewModel.UiState.Failed).message,
                        modifier = Modifier.align(Alignment.Center),
                    )
            }
        }
    }
}

@Composable
private fun KakaoMapView(
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit,
    onMapDestroyed: () -> Unit,
    onMapError: (Exception) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val currentOnMapDestroyed by rememberUpdatedState(onMapDestroyed)
    val currentOnMapError by rememberUpdatedState(onMapError)
    val mapView = remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).also { view ->
                mapView.value = view
                view.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = currentOnMapDestroyed()
                        override fun onMapError(error: Exception) = currentOnMapError(error)
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            currentOnMapReady(kakaoMap)
                        }
                    }
                )
            }
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.value?.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.value?.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // MapView.start()는 네이티브 GL 리소스를 띄우므로, AndroidView가 뷰를 그냥
            // detach하는 것만으로는 해제되지 않는다 — finish()로 명시적으로 정리해야 한다.
            mapView.value?.finish()
            mapView.value = null
        }
    }
}

private fun KakaoMap.drawStops(current: AppLatLng, stops: List<BusStop>) {
    val labelLayer = labelManager?.layer ?: return
    labelLayer.removeAll()

    val styles = labelManager?.addLabelStyles(LabelStyles.from(LabelStyle.from()))

    stops.forEach { stop ->
        val options = LabelOptions.from(
            LatLng.from(stop.position.latitude, stop.position.longitude)
        ).setStyles(styles)
        labelLayer.addLabel(options)
    }

    moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(current.latitude, current.longitude)))
}
