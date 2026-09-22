package com.jun.parknavi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jun.parknavi.ui.navigation.ParkNaviNavHost
import com.jun.parknavi.ui.theme.ParkNaviTheme
import dagger.hilt.android.AndroidEntryPoint

// 화면마다 자기 Scaffold(TopAppBar 포함)를 갖고 있으므로(StopMapScreen, StopListScreen),
// 여기서 또 Scaffold로 감싸면 TopAppBar가 중첩된다. 내비게이션 그래프만 올린다.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkNaviTheme {
                ParkNaviNavHost()
            }
        }
    }
}
