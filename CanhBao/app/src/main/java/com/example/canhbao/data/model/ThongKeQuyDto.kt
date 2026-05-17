package com.example.canhbao.data.model

import com.google.gson.annotations.SerializedName

data class DoiTienDtoRealtime(
    @SerializedName("userId")
    val userId: String = "Ẩn danh",
    val email: String = "",           // Nếu null, mặc định rỗng
    val soDiemDoi: Int = 0,
    val giaTri: Long = 0,
    val ngayDoi: String? = null
)

data class ThongKeQuyDto(
    val tongGiaTri: Long = 0,
    val lichSuVinhDanh: List<DoiTienDtoRealtime> = emptyList()
)