package com.jun.parknavi.data.repository

import com.jun.parknavi.data.model.BusStop
import com.jun.parknavi.data.model.LatLng

interface BusStopRepository {
    suspend fun getNearbyStops(from: LatLng): List<BusStop>
}
