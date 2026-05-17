package com.example.canhbao.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BaoCaoSuCoRetrofit {
    // 1. Tạo OkHttpClient với cấu hình Timeout mới
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Thời gian kết nối
        .readTimeout(60, TimeUnit.SECONDS)    // Thời gian chờ Server xử lý và trả về kết quả
        .writeTimeout(60, TimeUnit.SECONDS)   // Thời gian đẩy dữ liệu (ảnh) lên
        .build()

    val api: BaoCaoSuCoApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.HTTP_BASE_URL)
            .client(okHttpClient) // 2. Gắn client vào Retrofit
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaoCaoSuCoApi::class.java)
    }
}