package com.jun.parknavi.data.remote

import com.jun.parknavi.data.remote.dto.NearbyStopResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BusStopApi {
    // 국가대중교통정보센터(TAGO) 버스정류소정보 - 좌표기준 근접 정류소 목록조회
    // serviceKey는 TagoAuthInterceptor가 모든 요청에 자동으로 붙여준다.
    @GET("1613000/BusSttnInfoInqireService/getCrdntPrxmtSttnList")
    suspend fun getNearbyStops(
        @Query("gpsLati") latitude: Double,
        @Query("gpsLong") longitude: Double,
        @Query("numOfRows") numOfRows: Int = 30,
        @Query("pageNo") pageNo: Int = 1,
        @Query("_type") responseType: String = "json",
    ): NearbyStopResponse
}
