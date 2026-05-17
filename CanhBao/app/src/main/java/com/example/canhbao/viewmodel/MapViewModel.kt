package com.example.canhbao.viewmodel

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.canhbao.data.model.CameraMapDto
import com.example.canhbao.data.model.SuCoMapDto
import com.example.canhbao.data.model.TruSoMapDto
import com.example.canhbao.data.network.AppConfig
import com.example.canhbao.data.network.BaoCaoSuCoApi
import com.example.canhbao.data.network.BaoCaoSuCoRetrofit
import com.example.canhbao.ui.screens.createCameraIcon
import com.example.canhbao.ui.screens.createTruSoMarkerBitmap
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient

class MapViewModel : ViewModel() {
    // Thêm vào phía trên cùng của class MapViewModel
    private var mStompClient: StompClient? = null
    private val client = OkHttpClient()
    private val suCoApi: BaoCaoSuCoApi = BaoCaoSuCoRetrofit.api

    private val _startPoint = MutableStateFlow<Point?>(null)
    private val _endPoint = MutableStateFlow<Point?>(null)
    private val _startName = MutableStateFlow("")
    private val _endName = MutableStateFlow("")

    private val _routeDistance = MutableStateFlow("")
    val routeDistance: StateFlow<String> = _routeDistance

    private val _routeDuration = MutableStateFlow("")
    val routeDuration: StateFlow<String> = _routeDuration

    private val _travelMode = MutableStateFlow("Xe máy")
    val travelMode: StateFlow<String> = _travelMode

    private val _allRoutes = MutableStateFlow<List<List<Point>>>(emptyList())
    val allRoutes = _allRoutes.asStateFlow()

    private val _routeInfos = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    private val _selectedRouteIndex = MutableStateFlow(0)
    val selectedRouteIndex = _selectedRouteIndex.asStateFlow()

    val routePoints: StateFlow<List<Point>> =
        combine(_allRoutes, _selectedRouteIndex) { routes, index ->
            if (routes.isNotEmpty() && index < routes.size) routes[index] else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _suCoWithIcons = MutableStateFlow<List<Pair<SuCoMapDto, Bitmap>>>(emptyList())
    val suCoWithIcons = _suCoWithIcons.asStateFlow()


    // Thêm vào cùng chỗ với _suCoWithIcons
    private val _truSoWithIcons = MutableStateFlow<List<Pair<TruSoMapDto, Bitmap>>>(emptyList())
    val truSoWithIcons = _truSoWithIcons.asStateFlow()

    private val _placeSuggestions = MutableStateFlow<List<String>>(emptyList())
    val placeSuggestions: StateFlow<List<String>> = _placeSuggestions
    private val placePointMap = mutableMapOf<String, Point>()
    private val _truSoList = MutableStateFlow<List<TruSoMapDto>>(emptyList())
    val truSoList = _truSoList.asStateFlow()
    private val _cameraWithIcons = MutableStateFlow<List<Pair<CameraMapDto, Bitmap>>>(emptyList())
    val cameraWithIcons = _cameraWithIcons.asStateFlow()

    private val _sosResponse = MutableStateFlow<String?>(null)
    val sosResponse = _sosResponse.asStateFlow()

    // Hàm để xóa thông báo sau khi người dùng đã đọc
    fun clearSosResponse() {
        _sosResponse.value = null
    }
    // Thêm các list này để hỗ trợ tìm kiếm khi click
    private val _suCoList = MutableStateFlow<List<SuCoMapDto>>(emptyList())
    val suCoList = _suCoList.asStateFlow()

    // Cập nhật lại loadSuCoForMap để lưu cả list DTO
    fun loadSuCoForMap(context: Context) {
        viewModelScope.launch {
            try {
                val list = suCoApi.getSuCoForMap()
                _suCoList.value = list // Lưu list DTO gốc ở đây
                loadAllIcons(context, list)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun loadTruSoData(context: Context) {
        viewModelScope.launch {
            try {
                val list = suCoApi.getAllTruSo()
                // Sửa tên hàm ở đây cho khớp với hàm bạn đã sửa ở trên
                val icon = createTruSoMarkerBitmap(context)
                _truSoWithIcons.value = list.map { it to icon }
                println("🏠 Load API: Đã lấy ${list.size} trụ sở với icon mới")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadCameraData(context: Context) {
        viewModelScope.launch {
            try {
                // Đảm bảo interface BaoCaoSuCoApi đã cập nhật trả về List<CameraMapDto> với đủ trường
                val list = suCoApi.getAllCamera()
                val icon = createCameraIcon(context)

                // Lưu trọn vẹn list vào Flow để MapScreen sử dụng
                _cameraWithIcons.value = list.map { camera ->
                    camera to icon
                }
                Log.d("MapViewModel", "📸 Đã load ${list.size} camera. Ví dụ video: ${list.firstOrNull()?.videoUrl}")
            } catch (e: Exception) {
                Log.e("MapViewModel", "❌ Lỗi load camera: ${e.message}")
            }
        }
    }
    // Trong MapViewModel.kt
    private val _showSuCo = MutableStateFlow(true)
    val showSuCo = _showSuCo.asStateFlow()

    private val _showTruSo = MutableStateFlow(true)
    val showTruSo = _showTruSo.asStateFlow()

    private val _showCamera = MutableStateFlow(true)
    val showCamera = _showCamera.asStateFlow()

    fun toggleFilter(type: String) {
        when(type) {
            "SU_CO" -> _showSuCo.value = !_showSuCo.value
            "TRU_SO" -> _showTruSo.value = !_showTruSo.value
            "CAMERA" -> _showCamera.value = !_showCamera.value
        }
    }

    fun startListeningRealtime(context: Context) {
        // 1. Chỉ ngắt kết nối nếu đang thực sự kết nối

        try {
            if (mStompClient != null && mStompClient!!.isConnected) {
                mStompClient?.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Khởi tạo Client
        mStompClient =
            Stomp.over(Stomp.ConnectionProvider.OKHTTP, "${AppConfig.WS_BASE_URL}/ws-suco/websocket")
        subscribeToSosStatus(context)
        // 3. Đăng ký nhận tin nhắn (Thêm phần xử lý lỗi)
        mStompClient?.topic("/topic/su-co")?.subscribe({ topicMessage ->
            val jsonStr = topicMessage.payload
            try {
                val jsonObject = JSONObject(jsonStr)
                val trangThaiXuLy = jsonObject.optString("trangThaiXuLy", "")
                val id = jsonObject.optLong("id")

                // --- BƯỚC 1: NẾU HOÀN THÀNH THÌ XÓA KHỎI BẢN ĐỒ ---
                if (trangThaiXuLy == "HOAN_THANH") {
                    viewModelScope.launch(Dispatchers.Main) {
                        val currentList = _suCoWithIcons.value.toMutableList()
                        val removed = currentList.removeAll { it.first.id == id }
                        if (removed) {
                            _suCoWithIcons.value = currentList.toList()
                            println("🗑️ Realtime: Sự cố $id đã hoàn thành, tự động ẩn.")
                        }
                    }
                    return@subscribe // Thoát sớm, không chạy tiếp phần vẽ marker
                }

                // --- BƯỚC 2: XỬ LÝ CẬP NHẬT HOẶC THÊM MỚI ---
                val severity = jsonObject.optString("mucDoNghiemTrong").ifEmpty {
                    jsonObject.optString("severity", "LOW")
                }

                val newSuCo = SuCoMapDto(
                    id = id,
                    kinhDo = jsonObject.optDouble("kinhDo"),
                    viDo = jsonObject.optDouble("viDo"),
                    iconUrl = jsonObject.optString("iconUrl").ifEmpty {
                        jsonObject.optJSONObject("loaiSuCo")?.optString("iconUrl") ?: ""
                    },
                    mucDoNghiemTrong = severity,
                    moTa = jsonObject.optString("moTa", ""),
                    trangThaiDuyet = jsonObject.optString("trangThaiDuyet", ""),
                    trangThaiXuLy = trangThaiXuLy, // Lưu trạng thái để đồng bộ
                    hinhAnhUrl = jsonObject.optString("hinhAnhUrl", null)
                )

                if (newSuCo.kinhDo != 0.0 && newSuCo.viDo != 0.0) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val safeIconUrl = newSuCo.iconUrl ?: ""
                        val fullUrl = if (safeIconUrl.startsWith("http")) safeIconUrl
                        else "${AppConfig.HTTP_BASE_URL}$safeIconUrl"

                        // Vẽ lại icon (màu viền sẽ thay đổi theo mức độ nghiêm trọng mới)
                        loadMarkerIcon(context, fullUrl, newSuCo.mucDoNghiemTrong)?.let { bmp ->
                            withContext(Dispatchers.Main) {
                                val currentList = _suCoWithIcons.value.toMutableList()
                                // Xóa cái cũ dựa trên ID và thêm cái mới (với icon/màu mới)
                                currentList.removeAll { it.first.id == newSuCo.id }
                                currentList.add(newSuCo to bmp)
                                _suCoWithIcons.value = currentList.toList()
                                println("🎨 Realtime: Đã cập nhật mức độ/vị trí cho sự cố ${newSuCo.id}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { error ->
            println("❌ Lỗi Socket Sự cố: ${error.message}")
        })



        mStompClient?.topic("/topic/tru-so-delete")?.subscribe({ topicMessage ->
            val deletedId = topicMessage.payload.toLongOrNull()
            if (deletedId != null) {
                viewModelScope.launch(Dispatchers.Main) {
                    val currentList = _truSoWithIcons.value.toMutableList()
                    currentList.removeAll { it.first.id == deletedId }
                    _truSoWithIcons.value = currentList.toList()
                }
            }
        }, { it.printStackTrace() })

        // Thêm đoạn này vào bên trong startListeningRealtime, trước dòng mStompClient?.connect()

// --- 1. SOCKET CẬP NHẬT/THÊM CAMERA ---
        mStompClient?.topic("/topic/camera")?.subscribe({ topicMessage ->
            try {
                val jsonObject = JSONObject(topicMessage.payload)
                val newCam = CameraMapDto(
                    id = jsonObject.getLong("id"),
                    tenCamera = jsonObject.getString("tenCamera"),
                    kinhDo = jsonObject.getDouble("kinhDo"),
                    viDo = jsonObject.getDouble("viDo"),
                    moTa = jsonObject.optString("moTa", null),
                    anhCamera = jsonObject.optString("anhCamera", null),
                    videoUrl = jsonObject.optString("videoUrl", null)
                )
                viewModelScope.launch(Dispatchers.Main) {
                    val icon = createCameraIcon(context)
                    val currentList = _cameraWithIcons.value.toMutableList()
                    currentList.removeAll { it.first.id == newCam.id } // Xóa cũ nếu trùng ID
                    currentList.add(newCam to icon) // Thêm mới/cập nhật
                    _cameraWithIcons.value = currentList.toList()
                    println("🛰️ Realtime: Đã cập nhật Camera ${newCam.tenCamera} - Video: ${newCam.videoUrl}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { it.printStackTrace() })

// --- 2. SOCKET XÓA CAMERA (Phần bạn yêu cầu) ---
        mStompClient?.topic("/topic/camera-delete")?.subscribe({ topicMessage ->
            // Backend gửi payload là ID (kiểu Long)
            val deletedId = topicMessage.payload.toLongOrNull()
            if (deletedId != null) {
                viewModelScope.launch(Dispatchers.Main) {
                    val currentList = _cameraWithIcons.value.toMutableList()

                    // Tìm và xóa Camera có ID khớp với ID server gửi về
                    val removed = currentList.removeAll { it.first.id == deletedId }

                    if (removed) {
                        _cameraWithIcons.value = currentList.toList()
                        println("🗑️ Realtime: Đã xóa Camera ID $deletedId khỏi bản đồ")
                    }
                }
            }
        }, { it.printStackTrace() })

        mStompClient?.lifecycle()?.subscribe({ lifecycleEvent ->
            when (lifecycleEvent.type) {
                ua.naiksoftware.stomp.dto.LifecycleEvent.Type.OPENED -> println("✅ Stomp opened")
                ua.naiksoftware.stomp.dto.LifecycleEvent.Type.ERROR -> {
                    println("❌ Stomp error: ${lifecycleEvent.exception?.message}")
                }

                ua.naiksoftware.stomp.dto.LifecycleEvent.Type.CLOSED -> println("🔌 Stomp closed")
                else -> {}
            }
        }, { error ->
            // 🔥 DÒNG NÀY NGĂN CRASH KHI MẤT KẾT NỐI
            println("❌ Lỗi Lifecycle: ${error.message}")
        })

        mStompClient?.connect()
    }

    override fun onCleared() {
        super.onCleared()
        mStompClient?.disconnect() // Ngắt kết nối khi thoát app để tiết kiệm pin
    }

    private suspend fun processAndAddSuCo(context: Context, suCo: SuCoMapDto) {
        val path = suCo.iconUrl ?: ""
        val fullUrl = if (path.startsWith("http")) path
        else "${AppConfig.HTTP_BASE_URL}$path"

        if (suCo.trangThaiXuLy == "HOAN_THANH") {
            withContext(Dispatchers.Main) {
                val currentList = _suCoWithIcons.value.toMutableList()
                currentList.removeAll { it.first.id == suCo.id }
                _suCoWithIcons.value = currentList.toList()
            }
            return
        }
        loadMarkerIcon(context, fullUrl, suCo.mucDoNghiemTrong)?.let { bmp ->
            withContext(Dispatchers.Main) {
                val currentList = _suCoWithIcons.value.toMutableList()
                // Xóa cũ dựa trên ID, thêm mới (giúp cập nhật trạng thái ngay lập tức)
                currentList.removeAll { it.first.id == suCo.id }
                currentList.add(suCo to bmp)
                _suCoWithIcons.value = currentList.toList()
            }
        }
    }

    // --- SỬA LẠI HÀM LOAD BAN ĐẦU ---
    private suspend fun loadAllIcons(context: Context, list: List<SuCoMapDto>) {
        withContext(Dispatchers.IO) {
            // Thay vì viết lại logic, ta gọi hàm dùng chung cho từng item
            list.forEach { suCo ->
                processAndAddSuCo(context, suCo)
            }
            println("✅ Đã load xong ${list.size} sự cố")
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (query.length < 2) {
            _placeSuggestions.value = emptyList()
            return
        }
        viewModelScope.launch {
            val results = searchPlaces(query)
            _placeSuggestions.value = results.map { it.first }
            results.forEach { (name, point) ->
                placePointMap[name] = point
            }
        }
    }

    fun getPointForName(name: String): Point? = placePointMap[name]

    fun setTravelMode(mode: String) {
        _travelMode.value = mode
    }

    fun setEndPoint(point: Point, name: String) {
        _endPoint.value = point
        _endName.value = name
    }

    fun selectRoute(index: Int) {
        _selectedRouteIndex.value = index
        _routeInfos.value.getOrNull(index)?.let {
            _routeDistance.value = it.first
            _routeDuration.value = it.second
        }
    }

    fun clearRoute() {
        _allRoutes.value = emptyList()
        _startPoint.value = null
        _endPoint.value = null
        _routeDistance.value = ""
        _routeDuration.value = ""
        _selectedRouteIndex.value = 0
    }

    suspend fun searchPlaces(query: String): List<Pair<String, Point>> =
        withContext(Dispatchers.IO) {
            try {
                val url =
                    "https://nominatim.openstreetmap.org/search?q=$query&format=json&limit=5&countrycodes=vn"
                val request =
                    Request.Builder().url(url).header("User-Agent", "CanhBaoApp/1.0").build()
                val response = client.newCall(request).execute()
                val jsonArray = JSONArray(response.body?.string() ?: "[]")
                val results = mutableListOf<Pair<String, Point>>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    results.add(
                        obj.getString("display_name") to Point.fromLngLat(
                            obj.getDouble("lon"),
                            obj.getDouble("lat")
                        )
                    )
                }
                results
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun loadMarkerIcon(context: Context, url: String, mucDo: String?): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                result?.toBitmap()?.let { originalBitmap ->
                    val size = 160
                    val centerX = size / 2f
                    val circleRadius = 50f
                    val strokeThickness = 12f
                    val tailHeight = 25f
                    val padding = 15f
                    val output = Bitmap.createBitmap(
                        size,
                        (size + tailHeight).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(output)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    val strokeColor = when (mucDo?.uppercase()) {
                        "HIGH" -> Color.RED
                        "MEDIUM" -> Color.rgb(255, 165, 0)
                        "LOW" -> Color.GREEN
                        else -> Color.WHITE
                    }
                    val path = Path().apply {
                        moveTo(centerX - 22f, size - 28f)
                        lineTo(centerX + 22f, size - 28f)
                        lineTo(centerX, size + tailHeight - 2f)
                        close()
                    }
                    paint.color = strokeColor
                    canvas.drawPath(path, paint)
                    canvas.drawCircle(centerX, centerX, circleRadius + strokeThickness, paint)
                    paint.color = Color.WHITE
                    canvas.drawCircle(centerX, centerX, circleRadius, paint)
                    val innerSize = ((circleRadius - padding) * 2).toInt()
                    val left = (centerX - (innerSize / 2)).toInt()
                    val top = (centerX - (innerSize / 2)).toInt()
                    canvas.drawBitmap(
                        originalBitmap,
                        null,
                        Rect(left, top, left + innerSize, top + innerSize),
                        paint
                    )
                    output
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    // Thêm vào trong class MapViewModel
// Hàm này vẽ Marker có viền màu Blue cho Trụ sở

    fun getFreeDirections() {
        val start = _startPoint.value
        val end = _endPoint.value
        if (start == null || end == null) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // profile: car, bike, foot
                    val profile = when (_travelMode.value) {
                        "Xe máy" -> "bike"
                        "Ô tô" -> "car"
                        else -> "foot"
                    }

                    // URL OSRM: long,lat
                    val url = "https://router.project-osrm.org/route/v1/$profile/" +
                            "${start.longitude()},${start.latitude()};" +
                            "${end.longitude()},${end.latitude()}?overview=full&geometries=geojson&alternatives=true"

                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val jsonData = JSONObject(response.body?.string() ?: "")
                    val routes = jsonData.getJSONArray("routes")

                    val tempAllRoutes = mutableListOf<List<Point>>()
                    val tempRouteInfos = mutableListOf<Pair<String, String>>()

                    for (i in 0 until routes.length()) {
                        val route = routes.getJSONObject(i)

                        // Lấy tọa độ
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val points = mutableListOf<Point>()
                        for (j in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(j)
                            points.add(Point.fromLngLat(coord.getDouble(0), coord.getDouble(1)))
                        }
                        tempAllRoutes.add(points)

                        // Lấy khoảng cách và thời gian
                        val dist = route.getDouble("distance") / 1000.0 // km
                        val dur = route.getDouble("duration") / 60.0 // phút
                        tempRouteInfos.add(
                            String.format("%.1f km", dist) to String.format("%.0f phút", dur)
                        )
                    }

                    _allRoutes.value = tempAllRoutes
                    _routeInfos.value = tempRouteInfos

                    // Mặc định chọn tuyến đường đầu tiên
                    if (tempRouteInfos.isNotEmpty()) {
                        _selectedRouteIndex.value = 0
                        _routeDistance.value = tempRouteInfos[0].first
                        _routeDuration.value = tempRouteInfos[0].second
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Đảm bảo bạn có hàm này cho DiChuyenScreen gọi
    fun setStartPoint(point: Point, name: String) {
        _startPoint.value = point
        _startName.value = name
    }
    private val _currentHandlingTruSoId = MutableStateFlow<String?>(null)
    val currentHandlingTruSoId = _currentHandlingTruSoId.asStateFlow()
 fun startListeningWebRTC(userId: String, webrtcViewModel: WebRTCViewModel) {
     mStompClient?.topic("/topic/user/$userId/call")?.subscribe({ topicMessage ->
         val json = JSONObject(topicMessage.payload)

         // Phải truyền đủ 3 tham số như đã định nghĩa trong WebRTCViewModel
         mStompClient?.let { client ->
             webrtcViewModel.handleSignal(json, client, userId)
         }

     }, { it.printStackTrace() })
 }
    fun handleSosResponse(jsonRaw: String) {
        try {
            val json = JSONObject(jsonRaw)
            val message = json.optString("message")

            // Nếu Server trả về truSoId thì lấy, không thì mặc định hoặc lấy từ nguồn khác
            // Giả sử logic là: Nếu có idSOS mà chưa có truSoId, ta có thể tạm lưu để biết ca này đang active
            val truSoId = if (json.has("truSoId")) json.getString("truSoId") else "PENDING"

            _sosResponse.value = message
            _currentHandlingTruSoId.value = truSoId

            Log.d("MapViewModel", "Đã nhận phản hồi cứu hộ: $message từ Trụ sở: $truSoId")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    // Trong MapViewModel.kt
// Thêm hàm này để MapScreen lấy được client gửi đi
    val stompClient get() = mStompClient

    fun setupWebRTCSubscription(context: Context, webrtcViewModel: WebRTCViewModel) {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        // DÒNG NÀY RẤT QUAN TRỌNG ĐỂ DEBUG
        Log.i("WebRTC_Debug", "Android is listening for calls at: /topic/user/$userId/call")

        startListeningWebRTC(userId, webrtcViewModel)
    }
    private fun subscribeToSosStatus(context: Context) {
        // 1. Lấy đúng UID của người dùng hiện tại (Ví dụ dùng Firebase)
        // Nếu bạn không dùng Firebase, hãy lấy từ SharedPreferences nơi bạn lưu ID lúc đăng nhập
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        // 2. Địa chỉ phải khớp tuyệt đối với Logcat bạn đã thấy
        val topic = "/topic/user/$userId/sos-status"

        println("DEBUG_SOCKET: Đang lắng nghe tại $topic")

        mStompClient?.topic(topic)?.subscribe({ topicMessage ->
            val payload = topicMessage.payload
            println("DEBUG_SOCKET: ĐÃ NHẬN TIN: $payload")

            try {
                val jsonObject = JSONObject(payload)
                val message = jsonObject.optString("message")
                val status = jsonObject.optString("trangThai")

                viewModelScope.launch(Dispatchers.Main) {
                    // Cập nhật StateFlow để UI (MapScreen) tự hiển thị ElevatedCard
                    _sosResponse.value = message

                    // Tự ẩn sau 8 giây nếu hoàn thành
                    if (status == "HOAN_THANH" || status == "HUY_BO") {
                        kotlinx.coroutines.delay(8000)
                        _sosResponse.value = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, {
            println("❌ Lỗi kết nối Socket: ${it.message}")
        })
    }
    // --- THÊM VÀO ĐẦU CLASS MAPVIEWMODEL ---

    // Trạng thái Bật/Tắt
    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled = _isSoundEnabled.asStateFlow()

    private val _showRangeCircle = MutableStateFlow(true)
    val showRangeCircle = _showRangeCircle.asStateFlow()

    // Quản lý cấp độ cảnh báo để không đọc lặp lại (ID -> Level)
    private val alertLevels = mutableMapOf<Long, Int>()

    // Kênh truyền lệnh đọc giọng nói sang View
    private val _ttsCommand = MutableSharedFlow<String>()
    val ttsCommand = _ttsCommand.asSharedFlow()

    fun toggleSound() { _isSoundEnabled.value = !_isSoundEnabled.value }
    fun toggleRangeCircle() { _showRangeCircle.value = !_showRangeCircle.value }

    // Hàm kiểm tra khoảng cách (Sẽ gọi từ View mỗi khi GPS đổi)
    fun checkProximityAndAlert(userPoint: Point) {
        val currentSuCoList = _suCoWithIcons.value
        if (currentSuCoList.isEmpty()) return

        viewModelScope.launch {
            currentSuCoList.forEach { (suCo, _) ->
                val results = FloatArray(2)
                android.location.Location.distanceBetween(
                    userPoint.latitude(), userPoint.longitude(),
                    suCo.viDo, suCo.kinhDo,
                    results
                )
                val distance = results[0]
                val bearing = results[1]
                val huongDi = getDirectionName(bearing)
                val currentLevel = alertLevels[suCo.id] ?: 0

                // Làm tròn khoảng cách để đọc cho tự nhiên (ví dụ: 82.4m -> 82m)
                val roundedDistance = Math.round(distance)

                // Xử lý Tên loại và Mức độ như cũ
                val tenSuCo = if (suCo.tenLoai.isNullOrBlank() || suCo.tenLoai == "null") "sự cố" else suCo.tenLoai
                val mucDoRaw = suCo.mucDoNghiemTrong?.trim()?.uppercase()
                val mucDoText = when (mucDoRaw) {
                    "HIGH" -> "mức độ cao"
                    "MEDIUM" -> "mức độ trung bình"
                    "LOW" -> "mức độ thấp"
                    else -> ""
                }

                val warningMessage = if (mucDoText.isEmpty()) "có $tenSuCo" else "có $tenSuCo $mucDoText"

                when {
                    // Cảnh báo động: Đọc khoảng cách chính xác khi trong phạm vi 100m
                    // currentLevel < 1 đảm bảo nó chỉ nhắc 1 lần khi bắt đầu vào vùng cảnh báo
                    distance <= 100f && currentLevel < 1 -> {
                        if (_isSoundEnabled.value) {
                            // CÂU LỆNH MỚI: Đọc chính xác số mét
                            _ttsCommand.emit("Chú ý! Cách $roundedDistance mét $huongDi $warningMessage")
                        }
                        alertLevels[suCo.id] = 1
                    }

                    // Cảnh báo nhắc lại: Nếu cực gần (ví dụ 30m) thì nhắc lại 1 lần nữa
                    distance <= 30f && currentLevel < 2 -> {
                        if (_isSoundEnabled.value) {
                            _ttsCommand.emit("Cảnh báo! Chỉ còn $roundedDistance mét, $warningMessage")
                        }
                        alertLevels[suCo.id] = 2
                    }

                    // Reset khi đã đi xa
                    distance > 150f && currentLevel > 0 -> {
                        alertLevels.remove(suCo.id)
                    }
                }
            }
        }
    }
    // State lưu đối tượng cụ thể (DTO) để Screen hiển thị thông tin chi tiết
    private val _selectedObject = MutableStateFlow<Any?>(null)
    val selectedObject = _selectedObject.asStateFlow()

    // Hàm để Screen gọi khi người dùng click vào Marker trên bản đồ
    fun selectObject(obj: Any?) {
        _selectedObject.value = obj
    }

    // Hàm xóa đối tượng (khi đóng BottomSheet)
    fun clearSelectedObject() {
        _selectedObject.value = null
    }
    // Thêm vào MapViewModel.kt
    fun navigateToDestination(destPoint: Point, destName: String, userLocation: Point?) {
        if (userLocation == null) return

        // 1. Đặt điểm bắt đầu là vị trí hiện tại
        setStartPoint(userLocation, "Vị trí của bạn")

        // 2. Đặt điểm kết thúc là đối tượng vừa chọn (Trụ sở/Sự cố)
        setEndPoint(destPoint, destName)

        // 3. Gọi hàm tìm đường OSRM đã viết
        getFreeDirections()

        // 4. Xóa đối tượng đang chọn để đóng BottomSheet, tập trung vào bản đồ dẫn đường
        clearSelectedObject()
    }
    private fun getDirectionName(bearing: Float): String {
        return when {
            bearing in -22.5..22.5 -> "Phía Bắc"
            bearing in 22.5..67.5 -> "Hướng Đông Bắc"
            bearing in 67.5..112.5 -> "Phía Đông"
            bearing in 112.5..157.5 -> "Hướng Đông Nam"
            bearing in 157.5..180.0 || bearing in -180.0..-157.5 -> "Phía Nam"
            bearing in -157.5..-112.5 -> "Hướng Tây Nam"
            bearing in -112.5..-67.5 -> "Phía Tây"
            bearing in -67.5..-22.5 -> "Hướng Tây Bắc"
            else -> "Phía trước"
        }
    }

}
