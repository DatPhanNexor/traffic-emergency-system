package com.example.canhbao.data.model

data class DoiTienDto(
    val userId: String,
    val soDiemDoi: Int,
    val loaiDoi: String // "TIEN_MAT" hoặc "QUYEN_GOP"
)