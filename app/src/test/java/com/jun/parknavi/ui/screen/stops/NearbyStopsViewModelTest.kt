package com.jun.parknavi.ui.screen.stops

import android.util.Log
import app.cash.turbine.test
import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng
import com.jun.parknavi.data.repository.BusStopRepository
import com.jun.parknavi.location.LocationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NearbyStopsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<BusStopRepository>()
    private val locationProvider = mockk<LocationProvider>()

    private val here = LatLng(37.5665, 126.9780)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        // NearbyStopsViewModel은 실패 경로에서 android.util.Log.w()를 호출하는데,
        // 이건 순수 JVM 유닛 테스트에선 스텁이라 목킹 없이 부르면 예외를 던진다.
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `위치와 정류장 조회에 성공하면 Loaded 상태가 된다`() = runTest(dispatcher) {
        every { locationProvider.hasLocationPermission() } returns true
        coEvery { locationProvider.getCurrentLocation() } returns here
        val stops = listOf(BusStop("1", "정류소", here, 0))
        coEvery { repository.getNearbyStops(here) } returns stops

        val viewModel = NearbyStopsViewModel(repository, locationProvider)

        viewModel.uiState.test {
            assertEqualsState(NearbyStopsViewModel.UiState.Loading, awaitItem())
            viewModel.load()
            assertEqualsState(NearbyStopsViewModel.UiState.Loaded(here, stops), awaitItem())
        }
    }

    @Test
    fun `이미 Loaded 상태면 load()를 다시 호출해도 재조회하지 않는다`() = runTest(dispatcher) {
        // StopMapScreen/StopListScreen이 같은 ViewModel 인스턴스를 공유하므로, 이미 데이터가
        // 있는데 화면을 오가며 load()가 다시 호출돼도 GPS/네트워크를 또 쓰지 않아야 한다.
        every { locationProvider.hasLocationPermission() } returns true
        coEvery { locationProvider.getCurrentLocation() } returns here
        val stops = listOf(BusStop("1", "정류소", here, 0))
        coEvery { repository.getNearbyStops(here) } returns stops

        val viewModel = NearbyStopsViewModel(repository, locationProvider)

        viewModel.uiState.test {
            assertEqualsState(NearbyStopsViewModel.UiState.Loading, awaitItem())
            viewModel.load()
            assertEqualsState(NearbyStopsViewModel.UiState.Loaded(here, stops), awaitItem())

            viewModel.load()
            expectNoEvents()
        }
        coVerify(exactly = 1) { repository.getNearbyStops(here) }
    }

    @Test
    fun `위치 권한이 없으면 Failed 상태가 된다`() = runTest(dispatcher) {
        every { locationProvider.hasLocationPermission() } returns false

        val viewModel = NearbyStopsViewModel(repository, locationProvider)

        viewModel.uiState.test {
            awaitItem() // Loading (초기값)
            viewModel.load()
            val failed = awaitItem() as NearbyStopsViewModel.UiState.Failed
            assert(failed.message.contains("권한"))
        }
    }

    private fun assertEqualsState(
        expected: NearbyStopsViewModel.UiState,
        actual: NearbyStopsViewModel.UiState,
    ) {
        assert(expected == actual) { "expected=$expected actual=$actual" }
    }
}
