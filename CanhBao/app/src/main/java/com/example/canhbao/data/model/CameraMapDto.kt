package com.example.canhbao.data.model

data class CameraMapDto(
    val id: Long,
    val tenCamera: String,
    val kinhDo: Double,
    val viDo: Double,
    val anhCamera: String? = null, // Thêm để hứng videoUrl
    val videoUrl: String? = null,  // Thêm để hứng videoUrl
    val moTa: String? = null
)