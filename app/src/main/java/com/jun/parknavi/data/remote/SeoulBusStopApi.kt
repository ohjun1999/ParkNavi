package com.jun.parknavi.data.remote

import com.jun.parknavi.data.remote.dto.SeoulNearbyStationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SeoulBusStopApi {
    // 서울특별시_정류소정보조회 서비스 - 좌표기반 근접 정류소 조회.
    // serviceKey는 SeoulAuthInterceptor가 모든 요청에 자동으로 붙여준다.
    // tmX/tmY라는 이름과 달리 실제로는 TM 좌표가 아니라 WGS84 경도/위도를 그대로 받는다
    // (api.bus.go.kr 공식 문서에 "(WGS84)"로 명시돼 있고, 예제 URL도 십진수 위경도를 쓴다).
    @GET("stationinfo/getStaionsByPosList")
    suspend fun getNearbyStations(
        @Query("tmX") longitude: Double,
        @Query("tmY") latitude: Double,
        @Query("radius") radiusMeters: Int = 500,
    ): SeoulNearbyStationResponse
}
