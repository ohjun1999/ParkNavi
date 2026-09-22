package com.jun.parknavi.data.repository

import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng
import com.jun.parknavi.data.remote.BusStopApi
import com.jun.parknavi.data.remote.dto.BusStopDto
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BusStopRepositoryImpl @Inject constructor(
    private val api: BusStopApi,
) : BusStopRepository {

    override suspend fun getNearbyStops(from: LatLng): List<BusStop> {
        val response = api.getNearbyStops(latitude = from.latitude, longitude = from.longitude)
        val header = response.response.header
        check(header.resultCode == RESULT_CODE_SUCCESS) { header.resultMsg }

        val stops = response.response.body.items?.item.orEmpty()
        return stops
            .map { it.toDomain(from) }
            .sortedBy { it.distanceMeters }
    }

    private fun BusStopDto.toDomain(from: LatLng): BusStop {
        val position = LatLng(gpslati, gpslong)
        return BusStop(
            id = nodeid,
            name = nodenm,
            position = position,
            distanceMeters = distanceMeters(from, position),
        )
    }

    // android.location.Location.distanceBetween()는 안드로이드 프레임워크 의존이라
    // 순수 JVM 단위 테스트에서 스텁 예외를 던진다. 대신 하버사인 공식을 직접 계산해서
    // Repository를 안드로이드 프레임워크에 얽매이지 않는 순수 코틀린으로 유지한다.
    private fun distanceMeters(from: LatLng, to: LatLng): Int {
        val fromLatRad = Math.toRadians(from.latitude)
        val toLatRad = Math.toRadians(to.latitude)
        val deltaLatRad = Math.toRadians(to.latitude - from.latitude)
        val deltaLngRad = Math.toRadians(to.longitude - from.longitude)

        val a = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
            cos(fromLatRad) * cos(toLatRad) *
            sin(deltaLngRad / 2) * sin(deltaLngRad / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_METERS * c).toInt()
    }

    private companion object {
        const val RESULT_CODE_SUCCESS = "00"
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
