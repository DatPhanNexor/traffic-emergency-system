package com.example.canhbao.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canhbao.data.model.SuCoUserDto
import com.example.canhbao.data.network.AppConfig
import com.example.canhbao.data.network.BaoCaoSuCoRetrofit
import com.google.gson.Gson
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
class HomeViewModel : ViewModel() {
    var userDetail by mutableStateOf<SuCoUserDto?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private var mStompClient: StompClient? = null

    // Lấy thông tin ban đầu và kết nối Socket
    fun initUserData() {
        fetchUserInfo()
        connectUserSocket()
    }

    private fun fetchUserInfo() {
        viewModelScope.launch {
            isLoading = true
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val token = user.getIdToken(false).await().token ?: return@launch

                val response = BaoCaoSuCoRetrofit.api.getUserInfo("Bearer $token")
                userDetail = response

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    private fun connectUserSocket() {
        if (mStompClient?.isConnected == true) return

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        mStompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "${AppConfig.WS_BASE_URL}/ws-suco/websocket"
        )

        mStompClient?.topic("/topic/user-stats/$uid")?.subscribe(
            { topicMessage ->
                val updatedUser = Gson().fromJson(topicMessage.payload, SuCoUserDto::class.java)
                userDetail = updatedUser
            },
            { error ->
                error.printStackTrace()
            }
        )

        mStompClient?.connect()
    }

    override fun onCleared() {
        super.onCleared()
        mStompClient?.disconnect()
    }
}