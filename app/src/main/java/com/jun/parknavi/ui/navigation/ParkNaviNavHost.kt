package com.jun.parknavi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.jun.parknavi.ui.screen.stops.NearbyStopsViewModel
import com.jun.parknavi.ui.screen.stops.StopListScreen
import com.jun.parknavi.ui.screen.stops.StopMapScreen

// 화면이 늘어나도 이 파일 하나에서만 라우팅 구조를 파악할 수 있도록,
// 화면 전환(push/pop)은 전부 여기와 각 화면의 콜백 파라미터를 통해서만 이뤄지게 한다.
//
// StopMap/StopList를 StopsGraph라는 nav-graph로 묶고, 둘 다 StopsGraph의 NavBackStackEntry를
// ViewModelStoreOwner로 삼아 hiltViewModel()을 호출한다. 그래야 화면 전환 시마다 새
// NearbyStopsViewModel이 생기는 게 아니라 하나를 계속 공유해서, 지도에서 이미 불러온
// 위치/정류장 데이터를 목록 화면에서 다시 조회하지 않는다.
@Composable
fun ParkNaviNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = AppRoute.StopsGraph.route) {
        navigation(startDestination = AppRoute.StopMap.route, route = AppRoute.StopsGraph.route) {
            composable(AppRoute.StopMap.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AppRoute.StopsGraph.route)
                }
                val viewModel: NearbyStopsViewModel = hiltViewModel(parentEntry)
                StopMapScreen(
                    viewModel = viewModel,
                    onShowList = { navController.navigate(AppRoute.StopList.route) },
                )
            }
            composable(AppRoute.StopList.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(AppRoute.StopsGraph.route)
                }
                val viewModel: NearbyStopsViewModel = hiltViewModel(parentEntry)
                StopListScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
