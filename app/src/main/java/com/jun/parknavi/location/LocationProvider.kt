package com.jun.parknavi.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.jun.parknavi.data.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    // 위치 권한이 없으면 null을 돌려준다. 권한 요청 자체는 UI(화면) 책임이라
    // 여기서는 요청하지 않고, 있는지 없는지만 판단해서 호출부가 분기하게 한다.
    //
    // PRIORITY_HIGH_ACCURACY(GPS 우선)를 쓴다. PRIORITY_BALANCED_POWER_ACCURACY(네트워크
    // 기반 위치 우선)는 실기기 배터리엔 유리하지만, 이 앱은 애초에 "정확한 현재 위치 기준
    // 근처 정류장"이 핵심 기능이라 정확도를 배터리보다 우선한다. (에뮬레이터에서 실제로
    // BALANCED_POWER_ACCURACY로는 adb emu geo fix로 주입한 좌표를 무시하고 구글 기본
    // 위치(마운틴뷰)를 반환하는 걸 확인했다 — 실기기에서도 네트워크 위치는 GPS보다 부정확하다.)
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val location = fusedClient
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .await()
            ?: return null

        return LatLng(location.latitude, location.longitude)
    }
}
