package com.jun.parknavi.ui.screen.stops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jun.parknavi.data.model.BusStop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopListScreen(
    onBack: () -> Unit,
    viewModel: NearbyStopsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("주변 정류장 목록") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is NearbyStopsViewModel.UiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            is NearbyStopsViewModel.UiState.Failed -> {
                Box(Modifier.fillMaxSize().padding(padding)) {
                    Text(state.message, modifier = Modifier.align(Alignment.Center))
                }
            }
            is NearbyStopsViewModel.UiState.Loaded -> {
                StopList(stops = state.stops, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun StopList(stops: List<BusStop>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(stops, key = { it.id }) { stop ->
            ListItem(
                headlineContent = { Text(stop.name) },
                supportingContent = { Text("${stop.distanceMeters}m") },
                modifier = Modifier.fillMaxWidth(),
            )
            HorizontalDivider()
        }
    }
}
