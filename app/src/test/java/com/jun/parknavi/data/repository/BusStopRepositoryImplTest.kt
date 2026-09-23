package com.jun.parknavi.data.repository

import com.jun.parknavi.data.model.LatLng
import com.jun.parknavi.data.remote.ApiError
import com.jun.parknavi.data.remote.SeoulBusStopApi
import com.jun.parknavi.data.remote.dto.SeoulItemListDto
import com.jun.parknavi.data.remote.dto.SeoulMsgBodyDto
import com.jun.parknavi.data.remote.dto.SeoulMsgHeaderDto
import com.jun.parknavi.data.remote.dto.SeoulNearbyStationResponse
import com.jun.parknavi.data.remote.dto.SeoulStationDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BusStopRepositoryImplTest {

    private val api = mockk<SeoulBusStopApi>()
    private val repository = BusStopRepositoryImpl(api)

    // 서울시청 근처를 기준점으로, 더 가까운 정류소가 먼저 오는지 검증한다.
    private val from = LatLng(37.5665, 126.9780)

    @Test
    fun `가까운 정류소가 먼저 오도록 거리순으로 정렬한다`() = runTest {
        coEvery { api.getNearbyStations(any(), any(), any()) } returns success(
            far = SeoulStationDto("1", "먼 정류소", "0001", 127.0000, 37.6000),
            near = SeoulStationDto("2", "가까운 정류소", "0002", 126.9785, 37.5670),
        )

        val stops = repository.getNearbyStops(from)

        assertEquals(listOf("가까운 정류소", "먼 정류소"), stops.map { it.name })
    }

    @Test
    fun `headerCd가 실패면 ApiError Server를 던진다`() = runTest {
        coEvery { api.getNearbyStations(any(), any(), any()) } returns
            SeoulNearbyStationResponse(
                msgHeader = SeoulMsgHeaderDto(headerCd = "99", headerMsg = "서비스 오류"),
                msgBody = SeoulMsgBodyDto(itemList = null),
            )

        try {
            repository.getNearbyStops(from)
            fail("예외가 발생해야 한다")
        } catch (e: ApiError.Server) {
            assertEquals("99", e.resultCode)
            assertEquals("서비스 오류", e.resultMsg)
        }
    }

    @Test
    fun `필드가 빠진 레코드는 전체를 실패시키지 않고 건너뛴다`() = runTest {
        coEvery { api.getNearbyStations(any(), any(), any()) } returns success(
            far = SeoulStationDto(stationId = null, stationNm = "위경도 없는 정류소", arsId = null, gpsX = null, gpsY = null),
            near = SeoulStationDto("2", "가까운 정류소", "0002", 126.9785, 37.5670),
        )

        val stops = repository.getNearbyStops(from)

        assertEquals(listOf("가까운 정류소"), stops.map { it.name })
    }

    private fun success(far: SeoulStationDto, near: SeoulStationDto) = SeoulNearbyStationResponse(
        msgHeader = SeoulMsgHeaderDto(headerCd = "0", headerMsg = "정상적으로 처리되었습니다."),
        msgBody = SeoulMsgBodyDto(itemList = SeoulItemListDto(listOf(far, near))),
    )
}
