package com.example.canhbao.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canhbao.data.model.GoiDto
import com.example.canhbao.data.model.MuaGoiDto
import com.example.canhbao.data.model.MuaGoiRequest
import com.example.canhbao.data.network.AppConfig
import com.example.canhbao.data.network.BaoCaoSuCoApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient

class GoiViewModel(private val api: BaoCaoSuCoApi) : ViewModel() {
    private val _goiList = MutableStateFlow<List<GoiDto>>(emptyList())
    val goiList: StateFlow<List<GoiDto>> = _goiList

    private val _myPackages = MutableStateFlow<List<MuaGoiDto>>(emptyList())
    val myPackages: StateFlow<List<MuaGoiDto>> = _myPackages

    private var stompClient: StompClient? = null

    // Hàm kết nối Socket
    fun connectSocket() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val url = "${AppConfig.WS_BASE_URL}/ws-suco/websocket"
        if (stompClient?.isConnected == true) return

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, url)
        stompClient?.connect()

        stompClient?.topic("/topic/package-status/$uid")?.subscribe({
            val payload = it.payload

            viewModelScope.launch {
                if (payload == "REFRESH") {
                    fetchMyPackages()
                } else {
                    _socketErrorMessage.value = payload
                }
            }
        }, { it.printStackTrace() })
    }
    private val _socketErrorMessage = MutableStateFlow<String?>(null)
    val socketErrorMessage: StateFlow<String?> = _socketErrorMessage

    override fun onCleared() {
        // Fix lỗi crash Not Connected khi tắt ViewModel
        try {
            if (stompClient != null && stompClient!!.isConnected) {
                stompClient?.disconnect()
            }
        } catch (e: Exception) {
            Log.e("Socket", "Error during disconnect: ${e.message}")
        }
        super.onCleared()
    }

    // Các hàm fetch dữ liệu
    fun fetchGoi() {
        viewModelScope.launch {
            try {
                val response = api.getDanhSachGoi()
                _goiList.value = response
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun fetchMyPackages() {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val token = user.getIdToken(false).result?.token ?: return@launch

                val response = api.getGoiCuaToi("Bearer $token")
                _myPackages.value = response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearSocketError() { _socketErrorMessage.value = null }

    fun muaGoi(goiId: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val uid = user.uid
                val token = user.getIdToken(false).result?.token ?: ""

                val response = api.dangKyMuaGoi(
                    "Bearer $token",
                    MuaGoiRequest(goiId)
                )

                if (response.isSuccessful) {
                    fetchMyPackages()
                    onResult(true, null)
                } else {
                    onResult(false, response.errorBody()?.string())
                }

            } catch (e: Exception) {
                onResult(false, "Lỗi mạng")
            }
        }
    }

    fun huyGoi(idMuaGoi: Long, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val token = user.getIdToken(false).result?.token ?: ""

                val response = api.cancelMuaGoi("Bearer $token", idMuaGoi)

                if (response.isSuccessful) {
                    fetchMyPackages()
                    onResult(true, "Hủy thành công")
                } else {
                    onResult(false, response.errorBody()?.string())
                }
            } catch (e: Exception) {
                onResult(false, "Lỗi mạng")
            }
        }
    }
}