package com.example.canhbao.data.model

data class TinHieuSOSRequest(
    val viDo: Double,
    val kinhDo: Double,
    val ghiAmBase64: String? = null, // Chuỗi Base64 âm thanh
    val hinhAnhBase64: String,       // Chuỗi Base64 ảnh (Bắt buộc)
    val ghiChu: String? = null,
    val thoiGianTao: String,
    val diaChi: String? = null // Thêm dòng này
)