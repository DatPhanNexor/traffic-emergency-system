package com.example.canhbao.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canhbao.data.model.DoiQuaDto
import com.example.canhbao.data.model.QuaDto
import com.example.canhbao.data.model.SuCoUserDto
import com.example.canhbao.data.model.TuiDto
import com.example.canhbao.data.network.BaoCaoSuCoApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class QuaViewModel(private val api: BaoCaoSuCoApi) : ViewModel() {

    var allQua by mutableStateOf<List<QuaDto>>(emptyList())
    var filteredQua by mutableStateOf<List<QuaDto>>(emptyList())
    var userProfile by mutableStateOf<SuCoUserDto?>(null)
    var isLoading by mutableStateOf(false)
    var selectedTab by mutableStateOf("TẤT CẢ")
    private val auth = FirebaseAuth.getInstance()
    var listTuiQua by mutableStateOf<List<TuiDto>>(emptyList())
    var isTuiLoading by mutableStateOf(false)

    var isExchanging by mutableStateOf(false)

    // ================== LOAD DATA ==================
    fun loadData() {
        viewModelScope.launch {
            try {

                val user = auth.currentUser ?: return@launch
                val token = user.getIdToken(false).await().token ?: return@launch

                userProfile = api.getUserInfo("Bearer $token")

                allQua = api.getAllQua()
                filterQua(selectedTab)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ================== ĐỔI QUÀ ==================
    fun thucHienDoiQua(
        qua: QuaDto,
        onResult: (Boolean, String) -> Unit
    ) {
        if (isExchanging) return

        viewModelScope.launch {
            isExchanging = true
            try {
                val user = auth.currentUser ?: return@launch
                val token = user.getIdToken(false).await().token ?: return@launch

                val dto = DoiQuaDto(quaId = qua.id)

                val response = api.exchangeQua("Bearer $token", dto)

                if (response.isSuccessful) {
                    loadData()
                    onResult(true, "Đổi quà thành công!")
                } else {
                    onResult(false, "Đổi quà thất bại: ${response.message()}")
                }

            } catch (e: Exception) {
                onResult(false, "Lỗi kết nối: ${e.message}")
            } finally {
                isExchanging = false
            }
        }
    }

    // ================== FILTER ==================
    fun filterQua(loai: String) {
        selectedTab = loai
        filteredQua = if (loai == "TẤT CẢ") {
            allQua
        } else {
            allQua.filter { it.loai == loai }
        }
    }

    // ================== TÚI QUÀ ==================
    fun loadTuiQua() {
        viewModelScope.launch {
            isTuiLoading = true
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val token = user.getIdToken(false).await().token ?: return@launch

                listTuiQua = api.getMyGifts("Bearer $token")

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isTuiLoading = false
            }
        }
    }
}