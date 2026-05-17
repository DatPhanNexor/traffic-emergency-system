package com.example.canhbao.data.model
import java.math.BigDecimal
data class QuaDto(
    val id: Long,
    val ten: String,
    val loai: String, // SAN_PHAM hoặc VOUCHER
    val moTa: String?,
    val diem: Int,
    val hinhAnh: String?,
    val giaTriGiamPercent: Int?,
    val giaTriToiDa: Double?,
    val ngayKetThuc: String?,
    val trangThai: String? // HOAT_DONG / NGUNG
)