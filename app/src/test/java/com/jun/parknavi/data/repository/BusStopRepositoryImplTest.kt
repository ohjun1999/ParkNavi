package com.jun.parknavi.data.repository

import com.jun.parknavi.data.model.LatLng
import com.jun.parknavi.data.remote.BusStopApi
import com.jun.parknavi.data.remote.dto.BusStopDto
import com.jun.parknavi.data.remote.dto.ItemsDto
import com.jun.parknavi.data.remote.dto.NearbyStopResponse
import com.jun.parknavi.data.remote.dto.ResponseBodyDto
import com.jun.parknavi.data.remote.dto.ResponseHeaderDto
import com.jun.parknavi.data.remote.dto.ResponseWrapperDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BusStopRepositoryImplTest {

    private val api = mockk<BusStopApi>()
    private val repository = BusStopRepositoryImpl(api)

    // 서울시청 근처를 기준점으로, 더 가까운 정류소가 먼저 오는지 검증한다.
    private val from = LatLng(37.5665, 126.9780)

    @Test
    fun `가까운 정류소가 먼저 오도록 거리순으로 정렬한다`() = runTest {
        coEvery { api.getNearbyStops(any(), any(), any(), any(), any()) } returns success(
            far = BusStopDto("1", "먼 정류소", 37.6000, 127.0000),
            near = BusStopDto("2", "가까운 정류소", 37.5670, 126.9785),
        )

        val stops = repository.getNearbyStops(from)

        assertEquals(listOf("가까운 정류소", "먼 정류소"), stops.map { it.name })
    }

    @Test
    fun `resultCode가 실패면 예외를 던진다`() = runTest {
        coEvery { api.getNearbyStops(any(), any(), any(), any(), any()) } returns
            NearbyStopResponse(
                ResponseWrapperDto(
                    header = ResponseHeaderDto(resultCode = "99", resultMsg = "서비스 오류"),
                    body = ResponseBodyDto(items = null),
                )
            )

        try {
            repository.getNearbyStops(from)
            fail("예외가 발생해야 한다")
        } catch (e: IllegalStateException) {
            assertEquals("서비스 오류", e.message)
        }
    }

    private fun success(far: BusStopDto, near: BusStopDto) = NearbyStopResponse(
        ResponseWrapperDto(
            header = ResponseHeaderDto(resultCode = "00", resultMsg = "NORMAL SERVICE."),
            body = ResponseBodyDto(items = ItemsDto(listOf(far, near)), totalCount = 2),
        )
    )
}
