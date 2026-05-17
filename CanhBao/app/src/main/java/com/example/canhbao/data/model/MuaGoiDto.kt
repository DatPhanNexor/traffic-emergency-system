package com.example.canhbao.data.model

data class MuaGoiDto(
    val id: Long,
    val userId: String,
    val goiId: Long,
    val tenGoi: String, // Thêm để hiện tên gói
    val ngayMua: String,
    val ngayHetHan: String,
    val trangThai: String
)