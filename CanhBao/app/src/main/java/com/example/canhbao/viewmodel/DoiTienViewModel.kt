package com.example.canhbao.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canhbao.data.model.DoiTienDto
import com.example.canhbao.data.model.SuCoUserDto
import com.example.canhbao.data.model.ThongKeQuyDto
import com.example.canhbao.data.network.AppConfig
import com.example.canhbao.data.network.BaoCaoSuCoRetrofit
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient

enum class FilterMode { DAY, MONTH, YEAR }

class DoiTienViewModel : ViewModel() {
    var userDetail by mutableStateOf<SuCoUserDto?>(null)
        private set

    var isProcessing by mutableStateOf(false)
        private set

    var message by mutableStateOf("")
        private set
    var publicFundStats by mutableStateOf(ThongKeQuyDto())
        private set
    private var mStompClient: StompClient? = null

    // --- THÊM VÀO VIEWMODEL ---


    // Khai báo các biến state mới cho việc lọc
    var filterMode by mutableStateOf(FilterMode.DAY)
    var selectedDate by mutableStateOf(java.time.LocalDate.now())

    // Logic lọc danh sách (Dùng get() để nó tự cập nhật khi publicFundStats thay đổi)
    val filteredVinhDanh get() = publicFundStats.lichSuVinhDanh.filter { item ->
        val itemDate = item.ngayDoi?.take(10) ?: ""
        when (filterMode) {
            FilterMode.DAY -> itemDate == selectedDate.toString()
            FilterMode.MONTH -> itemDate.startsWith(selectedDate.toString().take(7))
            FilterMode.YEAR -> itemDate.startsWith(selectedDate.year.toString())
        }
    }

    // Tính tổng tiền theo danh sách đã lọc
    val totalFilteredValue get() = filteredVinhDanh.sumOf { it.giaTri ?: 0 }
    // Tiến tới 1 đơn vị thời gian (Ngày/Tháng/Năm)
    fun nextPeriod() {
        selectedDate = when (filterMode) {
            FilterMode.DAY -> selectedDate.plusDays(1)
            FilterMode.MONTH -> selectedDate.plusMonths(1)
            FilterMode.YEAR -> selectedDate.plusYears(1)
        }
    }

    // Lùi lại 1 đơn vị thời gian
    fun prevPeriod() {
        selectedDate = when (filterMode) {
            FilterMode.DAY -> selectedDate.minusDays(1)
            FilterMode.MONTH -> selectedDate.minusMonths(1)
            FilterMode.YEAR -> selectedDate.minusYears(1)
        }
    }
    fun init(uid: String) {
        fetchUserInfo(uid)
        fetchPublicFund() // Lấy dữ liệu quỹ hiện tại từ database
        connectUserSocket(uid)
    }
    private fun fetchPublicFund() {
        viewModelScope.launch {
            try {
                val stats = BaoCaoSuCoRetrofit.api.getThongKeQuy()
                publicFundStats = stats
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchUserInfo(uid: String) {
        viewModelScope.launch {
            try {
                userDetail = BaoCaoSuCoRetrofit.api.getUserInfo(uid)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun connectUserSocket(uid: String) {
        if (mStompClient?.isConnected == true) return
        mStompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, "${AppConfig.WS_BASE_URL}/ws-suco/websocket")

        // Lắng nghe điểm cá nhân
        mStompClient?.topic("/topic/user-stats/$uid")?.subscribe { userDetail = Gson().fromJson(it.payload, SuCoUserDto::class.java) }

        // Lắng nghe TỔNG QUỸ BIẾN ĐỘNG REALTIME
        // Trong connectUserSocket
        mStompClient?.topic("/topic/public-fund")?.subscribe {
            // Gson sẽ tự động map JSON vào ThongKeQuyDto,
            // bên trong đó lichSuVinhDanh sẽ là List<DoiTienDtoRealtime>
            publicFundStats = Gson().fromJson(it.payload, ThongKeQuyDto::class.java)
        }

        mStompClient?.connect()
    }

    // Hàm xử lý chung cho cả Đổi tiền và Quyên góp
    fun thucHienGiaoDich(uid: String, points: Int, type: String) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val dto = DoiTienDto(userId = uid, soDiemDoi = points, loaiDoi = type)
                val response = BaoCaoSuCoRetrofit.api.thucHienDoiTien(dto)

                if (response.isSuccessful && response.body() == true) {
                    message = if (type == "TIEN_MAT") "Đã quy đổi $points điểm!"
                    else "Cảm ơn bạn đã quyên góp!"

                    // === DÒNG QUAN TRỌNG NHẤT ===
                    fetchUserInfo(uid) // Gọi lại hàm này để lấy điểm mới ngay lập tức
                    // ============================
                } else {
                    message = "Giao dịch thất bại!"
                }
            } catch (e: Exception) {
                message = "Lỗi kết nối."
            } finally {
                isProcessing = false
                resetMessage()
            }
        }
    }


    private fun resetMessage() {
        viewModelScope.launch {
            delay(3000)
            message = ""
        }
    }

    override fun onCleared() {
        mStompClient?.disconnect()
        super.onCleared()
    }
}