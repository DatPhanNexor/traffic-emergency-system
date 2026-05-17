package com.example.canhbao.data.model

import com.google.gson.annotations.SerializedName


data class SuCoUserDto(
    val uid: String,
    val email: String?,
    val name: String?,
    val provider: String?,
    @SerializedName("totalPoints")
    val totalPoints: Int,

    @SerializedName("spamCount")
    val spamCount: Int,
    val tenGoi: String? = null
)

