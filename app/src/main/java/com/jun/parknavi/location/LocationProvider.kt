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
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasLocationPermission()) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val location = fusedClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .await()
            ?: return null

        return LatLng(location.latitude, location.longitude)
    }
}
