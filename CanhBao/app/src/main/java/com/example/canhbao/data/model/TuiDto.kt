package com.example.canhbao.data.model
import java.math.BigDecimal

data class TuiDto(
    val quaId: Long,
    val tenQua: String,
    val loai: String,
    val soLuong: Int,
    val giaTriGiamPercent: Int? = null,
    val giaTriToiDa: Double? = null,
    val ngayKetThuc: String? = null
)