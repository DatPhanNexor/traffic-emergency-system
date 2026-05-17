package com.example.canhbao.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MapRepository {

    fun mockLocation(): Flow<Pair<Double, Double>> = flow {
        val fakeRoute = listOf(
            10.7626 to 106.6601,
            10.7630 to 106.6605,
            10.7635 to 106.6610
        )

        for (point in fakeRoute) {
            emit(point)
            kotlinx.coroutines.delay(2000)
        }
    }
}
