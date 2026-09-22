package com.jun.parknavi.ui.screen.stops

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng
import com.jun.parknavi.data.remote.ApiError
import com.jun.parknavi.data.repository.BusStopRepository
import com.jun.parknavi.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// 지도 화면(StopMapScreen)과 목록 화면(StopListScreen)이 같은 데이터(현재 위치 + 주변 정류장)를
// 서로 다른 형태로 보여줄 뿐이라, ParkNaviNavHost가 두 화면을 같은 nav-graph에 묶어서 이
// ViewModel 인스턴스 하나를 공유하게 한다(hiltViewModel(parentEntry)). 그래서 지도 화면에서
// 이미 불러온 데이터를 목록 화면으로 넘어가도 다시 불러오지 않는다 — load()의 Loaded 가드 참고.
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
        if (_uiState.value is UiState.Loaded) return
        loadJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching {
                if (!locationProvider.hasLocationPermission()) {
                    error("위치 권한이 필요합니다.")
                }
                val location = locationProvider.getCurrentLocation()
                    ?: error("현재 위치를 확인할 수 없습니다. GPS 신호를 확인해 주세요.")
                location to repository.getNearbyStops(location)
            }.onSuccess { (location, stops) ->
                _uiState.value = UiState.Loaded(location, stops)
            }.onFailure { e ->
                Log.w(TAG, "주변 정류장 조회 실패", e)
                _uiState.value = UiState.Failed(errorMessage(e))
            }
        }
    }

    private fun errorMessage(e: Throwable): String = when (e) {
        is ApiError.Network -> "네트워크 연결을 확인해 주세요."
        is ApiError.Server -> "정류장 정보를 불러오지 못했습니다. (${e.resultCode})"
        is ApiError.Unknown -> "정류장 정보를 불러오지 못했습니다."
        else -> e.message ?: "정류장 정보를 불러오지 못했습니다."
    }

    private companion object {
        const val TAG = "NearbyStopsViewModel"
    }
}
