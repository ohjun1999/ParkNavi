package com.jun.parknavi.ui.navigation

sealed class AppRoute(val route: String) {
    // 지도/목록 화면이 NearbyStopsViewModel을 공유하기 위한 nav-graph 컨테이너 라우트.
    // ParkNaviNavHost 참고: 두 화면 다 hiltViewModel(navController.getBackStackEntry(StopsGraph.route))로
    // 이 라우트에 스코프를 맞춘다.
    object StopsGraph : AppRoute("stops_graph")
    object StopMap : AppRoute("stop_map")
    object StopList : AppRoute("stop_list")
}
