package com.jun.parknavi.ui.screen.stops

import app.cash.turbine.test
import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng
import com.jun.parknavi.data.repository.BusStopRepository
import com.jun.parknavi.location.LocationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
