package com.jun.parknavi.data.repository

import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng
import com.jun.parknavi.data.remote.ApiError
import com.jun.parknavi.data.remote.BusStopApi
import com.jun.parknavi.data.remote.dto.BusStopDto
import java.io.IOException
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class BusStopRepositoryImpl @Inject constructor(
    private val api: BusStopApi,
) : BusStopRepository {

    override suspend fun getNearbyStops(from: LatLng): List<BusStop> {
        val response = try {
            api.getNearbyStops(latitude = from.latitude, longitude = from.longitude)
        } catch (e: IOException) {
            throw ApiError.Network(e)
        } catch (e: Exception) {
            throw ApiError.Unknown(e)
        }

        val header = response.response.header
        if (header.resultCode != RESULT_CODE_SUCCESS) {
            throw ApiError.Server(header.resultCode, header.resultMsg)
        }

        val stops = response.response.body.items?.item.orEmpty()
        return stops
            .mapNotNull { it.toDomain(from) }
            .sortedBy { it.distanceMeters }
    }

    // 필드 하나라도 빠진(nodeid/gpslati/gpslong이 null인) 레코드는 지도에 표시할 수 없으니
    // 전체 목록을 실패시키지 않고 그 레코드만 건너뛴다.
    private fun BusStopDto.toDomain(from: LatLng): BusStop? {
        val id = nodeid ?: return null
        val lat = gpslati ?: return null
        val lng = gpslong ?: return null
        val position = LatLng(lat, lng)
        return BusStop(
            id = id,
            name = nodenm ?: "이름 없음",
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
