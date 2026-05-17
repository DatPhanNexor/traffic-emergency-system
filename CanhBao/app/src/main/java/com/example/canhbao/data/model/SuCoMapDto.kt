package com.example.canhbao.data.model

data class SuCoMapDto(
    val id: Long,
    val viDo: Double,
    val kinhDo: Double,
    val moTa: String? = null,
    val tenLoai: String? = null,
    val trangThaiDuyet: String? = null,
    val trangThaiXuLy: String? = null,
    val iconUrl: String? = null,
    val mucDoNghiemTrong: String? = null,
    val hinhAnhUrl: String? = null,

    // --- THÊM CÁC TRƯỜNG DƯỚI ĐÂY ĐỂ ĐỒNG BỘ VỚI BACKEND ---
    val tenDuong: String? = null,
    val quan: String? = null,
    val huyenHoac: String? = null,
    val thanhPho: String? = null,
    val diaChi: String? = null,
    val tenNguoiBao: String? = null
)