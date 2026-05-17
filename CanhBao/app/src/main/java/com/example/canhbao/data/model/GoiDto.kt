package com.example.canhbao.data.model
import java.math.BigDecimal

data class GoiDto(
    val id: Long,
    val ten: String,
    val thoiHan: Int,
    val gia: BigDecimal,
    val khoangCachMienPhi: Int,
    val uuDai: String
)

data class MuaGoiRequest(
    val goiId: Long
)