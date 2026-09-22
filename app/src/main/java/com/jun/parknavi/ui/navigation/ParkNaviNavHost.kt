package com.jun.parknavi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jun.parknavi.ui.screen.stops.StopListScreen
import com.jun.parknavi.ui.screen.stops.StopMapScreen

// 화면이 늘어나도 이 파일 하나에서만 라우팅 구조를 파악할 수 있도록,
// 화면 전환(push/pop)은 전부 여기와 각 화면의 콜백 파라미터를 통해서만 이뤄지게 한다.
@Composable
fun ParkNaviNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = AppRoute.StopMap.route) {
        composable(AppRoute.StopMap.route) {
            StopMapScreen(onShowList = { navController.navigate(AppRoute.StopList.route) })
        }
        composable(AppRoute.StopList.route) {
            StopListScreen(onBack = { navController.popBackStack() })
        }
    }
}
