package com.example.canhbao.data.network

import com.example.canhbao.BuildConfig

object AppConfig {

    const val PORT = 8080

    val HTTP_BASE_URL: String
        get() = "http://${BuildConfig.BASE_IP}:$PORT"

    val WS_BASE_URL: String
        get() = "ws://${BuildConfig.BASE_IP}:$PORT"
}