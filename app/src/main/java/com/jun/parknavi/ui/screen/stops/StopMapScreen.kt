package com.jun.parknavi.ui.screen.stops

import android.Manifest
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopMapScreen(
    onShowList: () -> Unit,
    viewModel: NearbyStopsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.load()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(uiState, kakaoMap) {
        val state = uiState
        val map = kakaoMap
        if (state is NearbyStopsViewModel.UiState.Loaded && map != null) {
            map.drawStops(state.currentLocation, state.stops)
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
                onMapReady = { kakaoMap = it },
            )
            if (uiState is NearbyStopsViewModel.UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun KakaoMapView(
    modifier: Modifier = Modifier,
    onMapReady: (KakaoMap) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMapReady by rememberUpdatedState(onMapReady)
    val mapView = remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).also { view ->
                mapView.value = view
                view.start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit
                        override fun onMapError(error: Exception) = Unit
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
