package com.example.canhbao.data.model

data class LoaiSuCoIdRequest(val id: Long)

data class BaoCaoSuCoRequest(
    val loaiSuCo: LoaiSuCoIdRequest,
    val kinhDo: Double,
    val viDo: Double,
    val moTa: String,
    val hinhAnhUrl: String, // Chuỗi Base64 hoặc URL ảnh
    val aiXacNhan: Boolean = true,
    val thoiGianTao: String // THÊM DÒNG NÀY ĐỂ KHỚP VỚI BACKEND
)