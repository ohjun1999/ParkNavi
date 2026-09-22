package com.jun.parknavi.ui.screen.stops

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng
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
// 서로 다른 형태로 보여줄 뿐이라, ViewModel 하나를 두 화면이 공유하지 않고 각자 인스턴스로 갖되
// 로직은 여기 하나로 모아둔다.
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
            runCatching {
                if (!locationProvider.hasLocationPermission()) {
                    error("위치 권한이 필요합니다.")
                }
                val location = locationProvider.getCurrentLocation()
                    ?: error("현재 위치를 확인할 수 없습니다.")
                location to repository.getNearbyStops(location)
            }.onSuccess { (location, stops) ->
                _uiState.value = UiState.Loaded(location, stops)
            }.onFailure { e ->
                _uiState.value = UiState.Failed(e.message ?: "정류장 정보를 불러오지 못했습니다.")
            }
        }
    }
}
