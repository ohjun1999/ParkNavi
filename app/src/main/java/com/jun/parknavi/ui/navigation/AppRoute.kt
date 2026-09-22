package com.jun.parknavi.ui.navigation

sealed class AppRoute(val route: String) {
    object StopMap : AppRoute("stop_map")
    object StopList : AppRoute("stop_list")
}
