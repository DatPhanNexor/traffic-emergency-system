package com.example.canhbao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.canhbao.navigation.NavGraph
import com.example.canhbao.viewmodel.AuthViewModel
import com.google.android.libraries.places.api.Places
import com.mapbox.common.MapboxOptions
import com.example.canhbao.BuildConfig
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        io.reactivex.plugins.RxJavaPlugins.setErrorHandler { throwable ->
            android.util.Log.e("STOMP_SAFE", "Bắt được lỗi ngầm, tránh văng App: ${throwable.message}")
        }
        MapboxOptions.accessToken =
            BuildConfig.MAPBOX_PUBLIC_TOKEN
        setContent {
            NavGraph(authViewModel = authViewModel)
        }
    }
}