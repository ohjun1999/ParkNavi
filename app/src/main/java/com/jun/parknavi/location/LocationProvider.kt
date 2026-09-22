package com.jun.parknavi.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.jun.parknavi.data.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedClient: FusedLocationProviderClient,
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
    //
    // CancellationTokenSource를 넘겨서 await()해야 이 코루틴이 취소될 때(타임아웃 포함)
    // Play Services 쪽 위치 요청도 같이 취소된다 — 토큰 없이 await()만 하면 코루틴은
    // 취소돼도 내부 GPS 요청은 백그라운드에서 계속 돈다. 실내/지하처럼 GPS가 안 잡히는
    // 상황에서 무기한 대기하지 않도록 타임아웃도 건다.
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null

        val cancellationTokenSource = CancellationTokenSource()
        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            fusedClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                .await(cancellationTokenSource)
        } ?: return null

        return LatLng(location.latitude, location.longitude)
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 15_000L
    }
}
