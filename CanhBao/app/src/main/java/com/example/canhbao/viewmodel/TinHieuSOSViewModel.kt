package com.example.canhbao.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canhbao.data.model.TinHieuSOSRequest
import com.example.canhbao.data.network.BaoCaoSuCoRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.location.Geocoder
import android.content.Context
import java.util.Locale

class TinHieuSOSViewModel : ViewModel() {
    private val _currentStep = MutableStateFlow(1)
    val currentStep = _currentStep.asStateFlow()

    var isSubmitting by mutableStateOf(false)
    var statusSOS by mutableStateOf("")

    var recordedFile by mutableStateOf<File?>(null)
    var noteText by mutableStateOf("")

    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    fun resetState() {
        _currentStep.value = 1
        isSubmitting = false
        statusSOS = ""
        noteText = ""
        recordedFile?.delete()
        recordedFile = null
    }

    fun nextStep() { _currentStep.value += 1 }
    fun prevStep() { if (_currentStep.value > 1) _currentStep.value -= 1 }

    // Tối ưu đọc file ở Background Thread
    private suspend fun fileToBase64(file: File): String = withContext(Dispatchers.IO) {
        android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP)
    }

    fun guiSOS(
        context: Context, // Thêm context vào đây để dùng Geocoder
        lat: Double,
        lng: Double,
        imageBitmap: android.graphics.Bitmap,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                isSubmitting = true
                statusSOS = "Đang xử lý dữ liệu..."

                // 1. Chuyển toàn bộ việc nặng (nén ảnh, đọc file) sang Dispatchers.IO
                val request = withContext(Dispatchers.IO) {
                    var diaChiTuMay = "Tọa độ: $lat, $lng"
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (addresses?.isNotEmpty() == true) {
                            diaChiTuMay = addresses[0].getAddressLine(0)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // Nén ảnh: Giảm xuống 60% chất lượng để gửi nhanh hơn (cứu hộ vẫn nhìn rõ)
                    val output = java.io.ByteArrayOutputStream()
                    imageBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, output)
                    val base64Image = android.util.Base64.encodeToString(output.toByteArray(), android.util.Base64.NO_WRAP)

                    // Xử lý âm thanh
                    val base64Audio = recordedFile?.let {
                        if (it.exists()) fileToBase64(it) else null
                    }

                    val time = java.time.LocalDateTime.now().toString()

                    TinHieuSOSRequest(
                        viDo = lat,
                        kinhDo = lng,
                        diaChi = diaChiTuMay,
                        ghiAmBase64 = base64Audio,
                        hinhAnhBase64 = base64Image,
                        ghiChu = noteText,
                        thoiGianTao = time
                    )
                }

                statusSOS = "Đang gửi lên máy chủ..."

                val user = auth.currentUser ?: run {
                    statusSOS = "Bạn chưa đăng nhập"
                    onComplete(false)
                    return@launch
                }

                val token = withContext(Dispatchers.IO) {
                    user.getIdToken(false).result?.token
                } ?: ""

                // 2. Gọi API trong Dispatchers.IO để không làm treo giao diện
                val response = withContext(Dispatchers.IO) {
                    BaoCaoSuCoRetrofit.api.submitSOS(
                        "Bearer $token",
                        request
                    )
                }

                if (response.isSuccessful) {
                    statusSOS = "Gửi thành công!"
                    onComplete(true)
                } else {
                    statusSOS = "Lỗi hệ thống: ${response.code()}"
                    onComplete(false)
                }
            } catch (e: Exception) {
                statusSOS = "Lỗi kết nối: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                isSubmitting = false
            }
        }
    }

}
