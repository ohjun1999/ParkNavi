package com.jun.parknavi.data.model

// 지도 SDK(카카오맵)에 종속되지 않는 앱 내부 좌표 타입.
// ViewModel/Repository는 이 타입만 알고, 특정 지도 SDK의 LatLng는 UI 레이어에서만 변환해서 쓴다.
data class LatLng(
    val latitude: Double,
    val longitude: Double,
)

data class BusStop(
    val id: String,
    val name: String,
    val position: LatLng,
    val distanceMeters: Int,
)
