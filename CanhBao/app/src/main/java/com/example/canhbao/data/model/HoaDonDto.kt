package com.example.canhbao.data.model

data class HoaDonDto(
    val id: Long,
    val sosId: Long,
    val trusoId: Long,
    val userId: String? = null,
    val tenSos: String,
    val noiDungXuLy: String? = null, // Khớp với trường 'xuLy' bên Java nếu cần
    val thanhTien: Double,           // Giá gốc
    val soTienGiam: Double? = 0.0,   // THÊM: Số tiền giảm
    val tongThanhToan: Double? = 0.0, // THÊM: Giá cuối cùng sau giảm giá
    val trangThai: String,           // PENDING hoặc PAID
    val createdAt: String? = null
)