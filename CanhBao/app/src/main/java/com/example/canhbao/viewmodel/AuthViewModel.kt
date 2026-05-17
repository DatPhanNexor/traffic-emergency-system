package com.example.canhbao.viewmodel
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canhbao.data.network.AppConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(
        val userEmail: String,
        val photoUrl: String? = null // thêm avatar URL
    ) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}


// Suspend function để Firebase signInWithCredential
suspend fun FirebaseAuth.signInWithCredentialSuspend(credential: AuthCredential): AuthResult =
    suspendCancellableCoroutine { cont ->
        signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) cont.resume(task.result!!)
            else cont.resumeWithException(task.exception ?: Exception("Auth Fail"))
        }
    }

// Suspend function để lấy idToken
suspend fun FirebaseUser.getIdTokenSuspend(forceRefresh: Boolean): GetTokenResult =
    suspendCancellableCoroutine { cont ->
        getIdToken(forceRefresh).addOnCompleteListener { task ->
            if (task.isSuccessful) cont.resume(task.result!!)
            else cont.resumeWithException(task.exception ?: Exception("Token Fail"))
        }
    }


class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Theo dõi User từ Firebase
    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    // Hàm kiểm tra lại (gọi khi cần)
    fun checkUserSession() {
        _user.value = auth.currentUser
    }

    // Cập nhật lại _user sau khi đăng nhập thành công
    fun onSignInSuccess(user: FirebaseUser?) {
        _user.value = user
    }
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState
    fun logout() {
        auth.signOut() // Đăng xuất khỏi Firebase
        _user.value = null
        _uiState.value = LoginUiState.Idle // Chuyển trạng thái về Idle để NavGraph kích hoạt chuyển trang
    }
    private suspend fun syncWithBackendSuspend(firebaseToken: String, email: String): Boolean {
        return withContext(Dispatchers.IO) { // Buộc chạy trên luồng IO
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply {
                    put("token", firebaseToken)
                    put("email", email)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${AppConfig.HTTP_BASE_URL}/api/auth/sync")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("BackendSync", "Response Code: ${response.code}")
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e("BackendSync", "Error: ${e.message}")
                false
            }
        }
    }

//    fun firebaseAuthWithGoogle(idToken: String) {
//        _uiState.value = LoginUiState.Loading
//
//        viewModelScope.launch {
//            try {
//                val credential = GoogleAuthProvider.getCredential(idToken, null)
//
//                // Firebase login
//                auth.signInWithCredentialSuspend(credential)
//                val user = auth.currentUser!!
//
//                _user.value = user
//
//                // Lấy idToken
//                val tokenResult = user.getIdTokenSuspend(true)
//                val firebaseToken = tokenResult.token ?: ""
//                val email = user.email ?: ""
//
//                // Gọi backend (dùng OkHttp như cũ, nhưng suspend)
//                val success = syncWithBackendSuspend(firebaseToken, email)
//                if (success) {
//                    _uiState.value = LoginUiState.Success(email, user.photoUrl?.toString())
//                } else {
//                    _uiState.value = LoginUiState.Error("Server Sync Fail")
//                }
//
//            } catch (e: Exception) {
//                _uiState.value = LoginUiState.Error(e.message ?: "Unknown Error")
//            }
//        }
//    }
fun firebaseAuthWithGoogle(idToken: String) {

    if (auth.currentUser != null) {
        _uiState.value = LoginUiState.Error("Bạn đã đăng nhập rồi!")
        return
    }
    if (auth.currentUser != null) {
        _uiState.value = LoginUiState.Error("Tài khoản ${auth.currentUser?.email} đang đăng nhập...")
        return
    }

    _uiState.value = LoginUiState.Loading

    viewModelScope.launch {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // 1. Firebase login
            auth.signInWithCredentialSuspend(credential)
            val user = auth.currentUser!!
            _user.value = user

            // 2. Lấy idToken từ Firebase
            val tokenResult = user.getIdTokenSuspend(true)
            val firebaseToken = tokenResult.token ?: ""
            val email = user.email ?: ""

            // --- THÊM DÒNG NÀY ĐỂ LẤY TOKEN TEST POSTMAN ---
            Log.d("TOKEN_CHO_POSTMAN", "------------------------------------------")
            Log.d("TOKEN_CHO_POSTMAN", firebaseToken)
            Log.d("TOKEN_CHO_POSTMAN", "------------------------------------------")
            // ----------------------------------------------

            // 3. Gọi backend sync
            val success = syncWithBackendSuspend(firebaseToken, email)
            if (success) {
                _uiState.value = LoginUiState.Success(email, user.photoUrl?.toString())
            } else {
                _uiState.value = LoginUiState.Error("Server Sync Fail")
            }

        } catch (e: Exception) {
            _uiState.value = LoginUiState.Error(e.message ?: "Unknown Error")
        }
    }
}


    private fun syncWithBackend(firebaseToken: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply {
                    put("token", firebaseToken)
                    put("email", email)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("http://192.168.1.7:8080/api/auth/sync")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d("API", "code=${response.code}, body=$responseBody")

                    if (response.isSuccessful) {
                        // Update trên Main thread
                        withContext(Dispatchers.Main) {
                            _uiState.value = LoginUiState.Success(email)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _uiState.value = LoginUiState.Error("Server Sync Error: ${response.code}")
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = LoginUiState.Error(e.message ?: "Unknown Error")
                }
            }
        }
    }

}