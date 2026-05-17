package com.example.canhbao.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.canhbao.viewmodel.MapViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import androidx.compose.material.icons.filled.Chat
// Cho hình ảnh và clip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage // Phải có thư viện Coil trong build.gradle
import com.example.canhbao.data.network.AppConfig
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    mapViewModel: MapViewModel,
    webrtcViewModel: com.example.canhbao.viewmodel.WebRTCViewModel, // Thêm dòng này
    isLoggedIn: Boolean,
    onReportClick: () -> Unit
) {
    val context = LocalContext.current
    val callState by webrtcViewModel.callState.collectAsState()
    val maThietBi = remember {
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }
    val focusManager = LocalFocusManager.current
    var mapView: MapView? by remember { mutableStateOf(null) }
    var lastUserLocation by remember { mutableStateOf<Point?>(null) }
    val routePoints by mapViewModel.routePoints.collectAsState()
    val distance by mapViewModel.routeDistance.collectAsState()
    val duration by mapViewModel.routeDuration.collectAsState()
    val mode by mapViewModel.travelMode.collectAsState()
    val allRoutes by mapViewModel.allRoutes.collectAsState()
    val selectedIndex by mapViewModel.selectedRouteIndex.collectAsState()
    val suggestions by mapViewModel.placeSuggestions.collectAsState()
    val suCoWithIcons by mapViewModel.suCoWithIcons.collectAsState()
    val truSoList by mapViewModel.truSoList.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val modeIcon = when (mode) {
        "Xe máy" -> Icons.Default.DirectionsBike
        "Ô tô" -> Icons.Default.DirectionsCar
        else -> Icons.Default.DirectionsWalk
    }

    // Thêm vào trong MapScreen



    val showRangeCircle by mapViewModel.showRangeCircle.collectAsState()
    var polygonManager: PolygonAnnotationManager? by remember { mutableStateOf(null) }

// Trong mapboxMap.loadStyleUri, thêm:
// polygonManager = annotations.createPolygonAnnotationManager()



    var pointManager: PointAnnotationManager? by remember { mutableStateOf(null) }
    var polylineManager: PolylineAnnotationManager? by remember { mutableStateOf(null) }
    var truSoManager: PointAnnotationManager? by remember { mutableStateOf(null) }

    val truSoWithIcons by mapViewModel.truSoWithIcons.collectAsState()
    // ... các khai báo cũ ...
    var cameraManager: PointAnnotationManager? by remember { mutableStateOf(null) }
    val cameraWithIcons by mapViewModel.cameraWithIcons.collectAsState()
    val sosMessage by mapViewModel.sosResponse.collectAsState()
    var userHeading by remember { mutableStateOf(0f) }

// Cập nhật listener trong AndroidView

    // Thay thế đoạn cũ của bạn bằng đoạn này
    val tts = remember {
        android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                println("TTS: Khởi tạo thành công!")
            } else {
                println("TTS: Khởi tạo thất bại với mã lỗi: $status")
            }
        }
    }

    // Lấy trạng thái từ ViewModel ở đầu MapScreen
    val showSuCo by mapViewModel.showSuCo.collectAsState()
    val showTruSo by mapViewModel.showTruSo.collectAsState()
    val showCamera by mapViewModel.showCamera.collectAsState()
    LaunchedEffect(tts) {
        // 1. Kiểm tra xem máy có hỗ trợ Tiếng Việt không
        val languages = tts.availableLanguages
        val hasVietnamese = languages?.any { it.language == "vi" } ?: false
        println("TTS: Danh sách ngôn ngữ máy có: ${languages?.take(5)}") // Xem 5 ngôn ngữ đầu tiên
        println("TTS: Máy có hỗ trợ tiếng Việt không? -> $hasVietnamese")

        // 2. Ép kiểu và kiểm tra kết quả setLanguage
        val langResult = tts.setLanguage(java.util.Locale("vi", "VN"))

        when (langResult) {
            android.speech.tts.TextToSpeech.LANG_MISSING_DATA -> println("TTS LỖI: Thiếu dữ liệu tiếng Việt (Cần tải về)")
            android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED -> println("TTS LỖI: Máy này không hỗ trợ tiếng Việt hoàn toàn")
            else -> println("TTS: Đã thiết lập tiếng Việt thành công!")
        }

        mapViewModel.ttsCommand.collect { message ->
            // 3. Gắn UtteranceProgressListener để biết lúc nào nó bắt đầu đọc
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { println("TTS: Bắt đầu đọc: $message") }
                override fun onDone(utteranceId: String?) { println("TTS: Đọc xong") }
                override fun onError(utteranceId: String?) { println("TTS LỖI: Lỗi khi đang phát âm thanh") }
            })

            tts.speak(message, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "MessageID")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!audioGranted) {
            Toast.makeText(context, "Bạn cần cấp quyền Micro để đàm thoại!", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }
    val scope = rememberCoroutineScope()

    // 2. Biến trạng thái (Đảm bảo đã khai báo ở đây)
    var isPressingSOS by remember { mutableStateOf(false) }

    var countdown by remember { mutableIntStateOf(5) }
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator

// Hiệu ứng làm mờ màn hình khi đang nhấn SOS
    val sosBlurAlpha by animateFloatAsState(
        targetValue = if (isPressingSOS) 0.6f else 0f,
        animationSpec = tween(500), label = "BlurAlpha"
    )
// Giải phóng TTS khi thoát app
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }
    val positionListener = remember {
        OnIndicatorPositionChangedListener { point ->
            lastUserLocation = point
            // Gọi logic check 100m bên ViewModel
            mapViewModel.checkProximityAndAlert(point)
        }
    }
    LaunchedEffect(lastUserLocation, showRangeCircle) {
        val manager = polygonManager ?: return@LaunchedEffect
        manager.deleteAll()
        if (showRangeCircle && lastUserLocation != null) {
            val circlePoints = createCirclePoints(lastUserLocation!!, 100.0)

            val polygonOptions = PolygonAnnotationOptions()
                .withPoints(listOf(circlePoints))
                .withFillColor("#4285F4")
                .withFillOpacity(0.15)
                // Thử xóa dòng withFillOutlineWidth nếu nó báo lỗi
                // Và sử dụng màu viền (nếu hỗ trợ)
                .withFillOutlineColor("#4285F4")

            manager.create(polygonOptions)
        }
    }
    LaunchedEffect(truSoWithIcons, truSoManager, showTruSo) {
        val manager = truSoManager ?: return@LaunchedEffect
        manager.deleteAll()

        if (showTruSo) { // Chỉ vẽ khi bật filter Trụ sở
            val options = truSoWithIcons.map { (ts, bmp) ->
                PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(ts.kinhDo, ts.viDo))
                    .withIconImage(bmp)
                    .withIconSize(1.0)
                    .withTextField(ts.tenTruSo)
                    .withTextOffset(listOf(0.0, 2.0))
                    .withTextColor(android.graphics.Color.rgb(0, 82, 204))
                    .withTextSize(12.0)
                    .withTextHaloColor(android.graphics.Color.WHITE)
                    .withTextHaloWidth(2.0)
            }
            manager.create(options)
            println("🏠 MapRender: Đã vẽ ${truSoWithIcons.size} Trụ sở")
        }
    }
    // Thêm khối này bên dưới LaunchedEffect của Trụ sở
    LaunchedEffect(cameraWithIcons, cameraManager, showCamera) {
        val manager = cameraManager ?: return@LaunchedEffect
        manager.deleteAll()

        if (showCamera) { // Chỉ vẽ khi bật filter Camera
            val options = cameraWithIcons.map { (cam, bmp) ->
                PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(cam.kinhDo, cam.viDo))
                    .withIconImage(bmp)
                    .withIconSize(1.0)
                    .withTextField(cam.tenCamera)
                    .withTextSize(10.0)
                    .withTextOffset(listOf(0.0, 2.0))
                    .withTextColor(android.graphics.Color.DKGRAY)
                    .withTextHaloColor(android.graphics.Color.WHITE)
                    .withTextHaloWidth(1.5)
            }
            manager.create(options)
            mapView?.invalidate()
            println("📸 MapRender: Đã vẽ ${cameraWithIcons.size} Camera")
        }
    }
    // --- MỚI: TỰ ĐỘNG LOAD DỮ LIỆU SỰ CỐ KHI VÀO MÀN HÌNH ---
    // Trong MapScreen.kt, sửa lại khối LaunchedEffect(Unit)
    LaunchedEffect(Unit) {
        // Chỉ load dữ liệu nếu danh sách đang trống (tránh load lại khi quay về từ tab khác)
        if (mapViewModel.suCoWithIcons.value.isEmpty()) {
            mapViewModel.loadSuCoForMap(context)
            mapViewModel.loadTruSoData(context)
            mapViewModel.loadCameraData(context)
        }

        // Quan trọng: Hàm này bên trong ViewModel cần có check "if (isConnected) return"
        // như tôi đã hướng dẫn ở phản hồi trước để không bị ngắt kết nối cũ.
        mapViewModel.startListeningRealtime(context)

        mapViewModel.setupWebRTCSubscription(context, webrtcViewModel)
    }

    var isMicroMuted by remember { mutableStateOf(false) }

// Reset lại micro mỗi khi trạng thái cuộc gọi thay đổi về IDLE hoặc bắt đầu cuộc gọi mới
    LaunchedEffect(callState) {
        if (callState == "IDLE") {
            isMicroMuted = false // Luôn mở lại mic cho cuộc gọi sau
        }
    }
    // Thêm vào trong MapScreen.kt
    val callerName by webrtcViewModel.callerName.collectAsState()
    val ringtonePlayer = remember {
        val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
        android.media.RingtoneManager.getRingtone(context, notification)
    }

// 2. Quản lý âm thanh (Ringtone + TTS)
    LaunchedEffect(callState) {
        if (callState == "INCOMING") {
            // Phát nhạc chuông hệ thống
            ringtonePlayer.play()

            // Đọc tên trụ sở lặp lại để người dùng nghe rõ
            val textToSpeak = "${callerName ?: "Trụ sở cứu hộ"} đang gọi tới. Nhắc lại, ${callerName ?: "Trụ sở cứu hộ"} đang gọi tới."
            tts.speak(textToSpeak, android.speech.tts.TextToSpeech.QUEUE_ADD, null, "RingtoneTTS")
        } else {
            // Dừng âm thanh ngay khi trạng thái thay đổi (đã nghe hoặc cúp máy)
            if (ringtonePlayer.isPlaying) ringtonePlayer.stop()
            tts.stop()
        }
    }
    // Lấy trạng thái từ ViewModel
    val selectedObject by mapViewModel.selectedObject.collectAsState()

    if (selectedObject != null) {
        // Đây là cái bảng "nhoi lên"
        ModalBottomSheet(
            onDismissRequest = { mapViewModel.clearSelectedObject() },
            sheetState = rememberModalBottomSheetState()
        ) {
            // Gọi hàm hiển thị thông tin chi tiết (Tôi đã viết ở câu trước)
            InfoDetailContent(
                data = selectedObject!!,
                userLocation = lastUserLocation,
                onNavigate = { point, name ->
                    mapViewModel.navigateToDestination(point, name, lastUserLocation)
                }
            )
        }
    }

// Giải phóng ringtone khi thoát màn hình
    DisposableEffect(Unit) {
        onDispose { ringtonePlayer.stop() }
    }



    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Map, null) }, label = { Text("Bản đồ") })
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        if (isLoggedIn) onReportClick()
                        else Toast.makeText(context, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
                    },
                    icon = {
                        Icon(
                            Icons.Default.AddAlert,
                            null,
                            tint = if (isLoggedIn) Color(0xFFD32F2F) else Color.Gray
                        )
                    },
                    label = { Text("Báo cáo") }
                )
                NavigationBarItem(selected = false, onClick = { navController.navigate("home") }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Cá nhân") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            // 1. MAPBOX MAP
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(106.6297, 10.8231))
                                .zoom(12.0)
                                .build()
                        )

                        // QUAN TRỌNG: Đợi Style load xong mới tạo Manager
                        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
                            mapboxMap.addOnMapClickListener {
                                focusManager.clearFocus() // Xóa dấu nháy khi click bất kỳ đâu trên bản đồ
                                true
                            }
                            val annotationApi = annotations // Khai báo biến local để sử dụng
                            pointManager = annotations.createPointAnnotationManager()
                            polylineManager = annotations.createPolylineAnnotationManager()
                            truSoManager = annotations.createPointAnnotationManager()
                            cameraManager = annotations.createPointAnnotationManager()
                            // 1. Click vào Sự cố
                            pointManager?.addClickListener(OnPointAnnotationClickListener { annotation ->
                                val clickedSuCo = suCoWithIcons.find {
                                    it.first.kinhDo == annotation.point.longitude() && it.first.viDo == annotation.point.latitude()
                                }?.first
                                mapViewModel.selectObject(clickedSuCo) // Bắn dữ liệu vào ViewModel để kích hoạt BottomSheet
                                true
                            })

                            // 2. Click vào Trụ sở
                            truSoManager?.addClickListener(OnPointAnnotationClickListener { annotation ->
                                val clickedTruSo = truSoWithIcons.find {
                                    it.first.kinhDo == annotation.point.longitude() && it.first.viDo == annotation.point.latitude()
                                }?.first
                                mapViewModel.selectObject(clickedTruSo)
                                true
                            })
                            // 3. Click vào Camera (Bổ sung cái này)
                            cameraManager?.addClickListener(OnPointAnnotationClickListener { annotation ->
                                // Tìm Camera trong list dựa trên tọa độ icon vừa bấm
                                val clickedCamera = cameraWithIcons.find {
                                    it.first.kinhDo == annotation.point.longitude() &&
                                            it.first.viDo == annotation.point.latitude()
                                }?.first

                                // Đẩy dữ liệu vào ViewModel để ModalBottomSheet hiện lên
                                if (clickedCamera != null) {
                                    mapViewModel.selectObject(clickedCamera)
                                }
                                true
                            })
                            // Sau khi manager sẵn sàng, ép vẽ lại dữ liệu cũ (nếu có)
                            polygonManager = annotationApi.createPolygonAnnotationManager()
                            val currentSuCo = suCoWithIcons
                            if (currentSuCo.isNotEmpty()) {
                                pointManager?.deleteAll()
                                currentSuCo.forEach { (suCo, bmp) ->
                                    val opt = PointAnnotationOptions()
                                        .withPoint(Point.fromLngLat(suCo.kinhDo, suCo.viDo))
                                        .withIconImage(bmp)
                                    pointManager?.create(opt)
                                }
                            }
                        }

                        // Trong factory của AndroidView
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                            location.updateSettings {
                                enabled = true
                                pulsingEnabled = true
                            }

                            // QUAN TRỌNG: Thêm dòng này để la bàn bắt đầu gửi dữ liệu xoay
                            location.addOnIndicatorBearingChangedListener { bearing ->
                                println("Compass_Debug: $bearing") // Nếu thấy số chạy trong Logcat là thành công
                                userHeading = bearing.toFloat()
                            }

                            location.addOnIndicatorPositionChangedListener(positionListener)
                        }
                        mapView = this
                    }
                }
            )

            // 2. VẼ MARKER SỰ CỐ
            LaunchedEffect(suCoWithIcons, pointManager, showSuCo) {
                val manager = pointManager ?: return@LaunchedEffect
                try {
                    manager.deleteAll()

                    if (showSuCo) { // Chỉ vẽ khi bật filter Sự cố
                        val optionsList = suCoWithIcons.map { (suCo, bmp) ->
                            PointAnnotationOptions()
                                .withPoint(Point.fromLngLat(suCo.kinhDo, suCo.viDo))
                                .withIconImage(bmp)
                        }
                        if (optionsList.isNotEmpty()) {
                            manager.create(optionsList)
                        }
                        mapView?.invalidate()
                        println("🎨 MapRender: Đã vẽ ${suCoWithIcons.size} Marker Sự cố")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. VẼ TUYẾN ĐƯỜNG
            LaunchedEffect(allRoutes, selectedIndex) {
                polylineManager?.deleteAll()
                allRoutes.forEachIndexed { idx, pts ->
                    val color = if (idx == selectedIndex) "#4285F4" else "#888888"
                    polylineManager?.create(PolylineAnnotationOptions().withPoints(pts).withLineColor(color).withLineWidth(5.0))
                }
            }

            // 4. SEARCH UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Đẩy xuống dưới thanh trạng thái hệ thống
            ) {
                if (routePoints.isEmpty()) {
                    // Thanh tìm kiếm
                    // Trong MapScreen.kt (Chỗ gọi SearchUI)
                    SearchUI(
                        searchText = searchText,
                        onValueChange = {
                            searchText = it
                            mapViewModel.onSearchQueryChanged(it)
                        },
                        suggestions = suggestions,
                        onSelected = { name ->
                            mapViewModel.getPointForName(name)?.let { pt ->
                                mapView?.camera?.easeTo(
                                    CameraOptions.Builder().center(pt).zoom(15.0).build(),
                                    MapAnimationOptions.mapAnimationOptions { duration(1200) }
                                )
                            }
                            focusManager.clearFocus()
                        },
                        onMyLocationClick = {
                            // Logic zoom lại vị trí cũ của bạn
                            val currentPoint = lastUserLocation
                            if (currentPoint != null) {
                                mapView?.camera?.easeTo(
                                    CameraOptions.Builder().center(currentPoint).zoom(16.0).build(),
                                    MapAnimationOptions.mapAnimationOptions { duration(1000) }
                                )
                            } else {
                                Toast.makeText(context, "Đang chờ GPS...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    // 2. THANH CHỌN LỚP BẢN ĐỒ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), // Khoảng cách giữa các ô
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. CHIP SỰ CỐ (Màu Đỏ)
                        FilterChip(
                            modifier = Modifier.weight(1f), // Căn đều hàng
                            selected = showSuCo,
                            onClick = { mapViewModel.toggleFilter("SU_CO") },
                            label = { Text("Sự cố", fontSize = 11.sp, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color.Red,
                                iconColor = Color.Red,
                                selectedContainerColor = Color.Red,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = showSuCo,
                                borderColor = Color.Red,
                                borderWidth = 1.dp
                            )
                        )

                        // 2. CHIP TRỤ SỞ (Màu Xanh Dương)
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = showTruSo,
                            onClick = { mapViewModel.toggleFilter("TRU_SO") },
                            label = { Text("Trụ sở", fontSize = 11.sp, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Business, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color(0xFF2196F3),
                                iconColor = Color(0xFF2196F3),
                                selectedContainerColor = Color(0xFF2196F3),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = showTruSo,
                                borderColor = Color(0xFF2196F3),
                                borderWidth = 1.dp
                            )
                        )

                        // 3. CHIP CAMERA (Màu Đen)
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = showCamera,
                            onClick = { mapViewModel.toggleFilter("CAMERA") },
                            label = { Text("Camera", fontSize = 11.sp, maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.Videocam, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Color.Black,
                                iconColor = Color.Black,
                                selectedContainerColor = Color.Black,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = showCamera,
                                borderColor = Color.Black,
                                borderWidth = 1.dp
                            )
                        )
                    }
                }
            }
            // Chèn vào cuối Box chính của MapScreen.kt


            // 5. NHÓM NÚT ĐIỀU KHIỂN
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Tìm đến đoạn nút Nhấc máy (FloatingActionButton)
                if (callState == "INCOMING") {
                    FloatingActionButton(
                        onClick = {
                            // Đảm bảo mapViewModel.stompClient không bị null khi truyền vào
                            val currentStompClient = mapViewModel.stompClient

                            // Gọi hàm với đầy đủ 2 tham số như định nghĩa trong ViewModel
                            webrtcViewModel.acceptCall(currentStompClient, maThietBi)

                            Toast.makeText(context, "Đã kết nối đàm thoại", Toast.LENGTH_SHORT).show()
                        },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Call, "Nhấc máy")
                    }
                }


                val isCircleVisible by mapViewModel.showRangeCircle.collectAsState()
                SmallFloatingActionButton(
                    onClick = { mapViewModel.toggleRangeCircle() },
                    containerColor = if (isCircleVisible) Color(0xFF4285F4) else Color.White,
                    contentColor = if (isCircleVisible) Color.White else Color.Gray,
                    shape = CircleShape
                ) { Icon(Icons.Default.BlurCircular, "Vòng 100m") }

// Nút Âm thanh
                val isAudioOn by mapViewModel.isSoundEnabled.collectAsState()
                SmallFloatingActionButton(
                    onClick = { mapViewModel.toggleSound() },
                    containerColor = if (isAudioOn) Color(0xFF4CAF50) else Color.White,
                    contentColor = if (isAudioOn) Color.White else Color.Gray,
                    shape = CircleShape
                ) { Icon(if (isAudioOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff, "Giọng nói") }


                FloatingActionButton(
                    onClick = {
                        navController.navigate("di_chuyen")
                    },
                    containerColor = Color(0xFF4285F4), // Màu xanh dương đặc trưng của Directions
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Directions, "Chỉ đường")
                }
            }

            // 6. ROUTE INFO
            if (routePoints.isNotEmpty()) {
                RouteInfoUI(mode = mode, icon = modeIcon, duration = duration, distance = distance, onBack = { mapViewModel.clearRoute() }, onStartNav = {})
            }
            // 7. NÚT SOS CHÍNH GIỮA (ĐÈ 5 GIÂY)
            // ... bên trong Box của Scaffold, phía dưới Column (điểm số 5) ...

// 7. NÚT SOS CHÍNH GIỮA (ĐÈ 5 GIÂY)
            var isPressingSOS by remember { mutableStateOf(false) }
            val sosScale by animateFloatAsState(
                targetValue = if (isPressingSOS) 2.5f else 1f,
                animationSpec = if (isPressingSOS) tween(5000, easing = LinearEasing) else tween(300),
                label = "SOSScale"
            )

            // --- TRONG MapScreen.kt (Vị trí số 7) ---

            sosMessage?.let { message -> // Dùng đúng tên biến đã khai báo ở đầu hàm
                val handlingTruSoId by mapViewModel.currentHandlingTruSoId.collectAsState()
                ElevatedCard(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFFE8F5E9) // Màu xanh lá nhẹ tạo cảm giác an tâm
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon xác thực/an toàn
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HỆ THỐNG PHẢN HỒI",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                            // THÊM NÚT NÀY:
                            if (handlingTruSoId != null && callState == "IDLE") {
                                Button(
                                    onClick = {
                                        val myId = maThietBi
                                        val client = mapViewModel.stompClient

                                        // Xử lý ID an toàn: Tránh việc cộng chuỗi "TRU_SO_" hai lần
                                        val rawId = handlingTruSoId!!
                                        val targetId = if (rawId.startsWith("TRU_SO_")) rawId else "TRU_SO_$rawId"

                                        if (client != null && client.isConnected) {
                                            webrtcViewModel.startCall(client, targetId, myId)
                                        } else {
                                            Toast.makeText(context, "Kết nối máy chủ đang bận, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                                            mapViewModel.startListeningRealtime(context) // Thử kết nối lại
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.Call, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("GỌI CHO TRỤ SỞ TIẾP NHẬN")
                                }
                            }
                        }
                        // Tìm đến nút tắt thông báo ở cuối Card SOS
                        IconButton(onClick = {
                            webrtcViewModel.endCall() // Ngắt kết nối WebRTC
                            mapViewModel.clearSosResponse() // Tắt Card thông báo trên màn hình
                        }) {
                            Icon(Icons.Default.CallEnd, null, tint = Color.Red)
                        }
                    }
                }
            }


            if (isPressingSOS) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = sosBlurAlpha))
                        .zIndex(5f) // Gán chỉ số lớp để chắc chắn nó đè lên bản đồ
                        .pointerInput(Unit) {} // Chặn click xuống dưới
                )

                // --- 2. SỐ ĐẾM NGƯỢC (Cũng nằm trên lớp phủ) ---
                Box(
                    modifier = Modifier.fillMaxSize().zIndex(6f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdown.toString(),
                        modifier = Modifier.offset(y = (-100).dp),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 130.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Red,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black,
                                offset = androidx.compose.ui.geometry.Offset(5f, 5f),
                                blurRadius = 10f
                            )
                        )
                    )
                }
            }
            // --- NÚT SOS ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .size(80.dp)
                    .scale(sosScale) // sosScale bạn đã có animateFloatAsState(2.5f)
                    .background(if (isPressingSOS) Color.Red else Color(0xFFD32F2F), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressingSOS = true
                                countdown = 5

                                // Logic chạy số đếm ngược
                                val timerJob = kotlinx.coroutines.MainScope().launch {
                                    for (i in 5 downTo 1) {
                                        countdown = i
                                        kotlinx.coroutines.delay(1000)
                                    }
                                }

                                val startTime = System.currentTimeMillis()
                                try {
                                    val released = try { awaitRelease(); true } catch (e: Exception) { false }
                                    val duration = System.currentTimeMillis() - startTime

                                    timerJob.cancel() // Dừng đếm nếu thả tay sớm

                                    if (released && duration >= 5000) {
                                        // RUNG MẠNH KHI THÀNH CÔNG
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                        } else {
                                            vibrator.vibrate(500)
                                        }

                                        val lat = lastUserLocation?.latitude() ?: 0.0
                                        val lng = lastUserLocation?.longitude() ?: 0.0
                                        if (lat != 0.0) {
                                            navController.navigate("tin_hieu_sos/$lat/$lng")
                                        }
                                    } else if (released) {
                                        Toast.makeText(context, "Giữ thêm một chút nữa!", Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    isPressingSOS = false
                                    countdown = 5
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPressingSOS) "THẢ ĐỂ GỬI" else "SOS",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isPressingSOS) 10.sp else 18.sp
                )
            }

            if (callState != "IDLE") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)) // Làm mờ 85% để tập trung vào cuộc gọi
                        .pointerInput(Unit) { detectTapGestures { /* Chặn mọi tương tác với bản đồ phía dưới */ } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Hiệu ứng vòng tròn lan tỏa (Pulse Animation)
                        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                            label = "Scale"
                        )

                        Box(contentAlignment = Alignment.Center) {
                            // Vòng tròn mờ phía sau
                            Box(
                                Modifier
                                    .size(140.dp)
                                    .scale(pulseScale)
                                    .background(
                                        if (callState == "CONNECTED") Color.Green.copy(0.2f) else Color.Red.copy(0.2f),
                                        CircleShape
                                    )
                            )

                            // Icon chính
                            Surface(
                                shape = CircleShape,
                                color = if (callState == "CONNECTED") Color(0xFF4CAF50) else Color(0xFFFF9800),
                                modifier = Modifier.size(100.dp),
                                shadowElevation = 10.dp
                            ) {
                                Icon(
                                    imageVector = if (callState == "CONNECTED") Icons.Default.Call else Icons.Default.RingVolume,
                                    contentDescription = null,
                                    modifier = Modifier.padding(25.dp).size(50.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Tên trụ sở
                        Text(
                            text = (callerName ?: "TRỤ SỞ CỨU HỘ").uppercase(),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )

                        Text(
                            text = when(callState) {
                                "INCOMING" -> "CUỘC GỌI ĐẾN..."
                                "CONNECTED" -> "ĐANG ĐÀM THOẠI"
                                else -> "ĐANG KẾT NỐI"
                            },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(80.dp))

                        // Hàng nút hành động
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Nút Cúp máy / Từ chối (Luôn hiện)
                            ActionCallButton(
                                icon = Icons.Default.CallEnd,
                                label = if (callState == "INCOMING") "Từ chối" else "Kết thúc",
                                color = Color.Red,
                                onClick = {
                                    // Truyền thêm tham số để báo cho Web biết
                                    webrtcViewModel.endCall(mapViewModel.stompClient, maThietBi)
                                    Toast.makeText(context, "Đã kết thúc cuộc gọi", Toast.LENGTH_SHORT).show()
                                }
                            )
                            // 2. NÚT CHẶN ÂM (MUTE) - Chỉ hiện khi đã kết nối (CONNECTED)
                            // 2. NÚT CHẶN ÂM (MUTE) - Chỉ hiện khi đã kết nối (CONNECTED)
                            if (callState == "CONNECTED") {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FloatingActionButton(
                                        onClick = {
                                            // Đảo trạng thái logic UI
                                            isMicroMuted = !isMicroMuted
                                            // Truyền THẲNG trạng thái đó vào ViewModel (ViewModel sẽ xử lý setEnabled(!isMuted))
                                            webrtcViewModel.toggleMic(isMicroMuted)
                                        },
                                        // Màu sắc thay đổi theo trạng thái ĐÃ tắt hay ĐANG mở
                                        containerColor = if (isMicroMuted) Color.Red.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White,
                                        shape = CircleShape,
                                        modifier = Modifier.size(60.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isMicroMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                    Text(
                                        text = if (isMicroMuted) "Đã tắt mic" else "Chặn âm",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            // Nút Trả lời (Chỉ hiện khi đang đổ chuông)
                            if (callState == "INCOMING") {
                                ActionCallButton(
                                    icon = Icons.Default.Call,
                                    label = "Trả lời",
                                    color = Color(0xFF4CAF50),
                                    onClick = {
                                        webrtcViewModel.acceptCall(mapViewModel.stompClient, maThietBi)
                                    }
                                )
                            }
                        }
                    }
                }
                // 8. BOTTOM SHEET CHI TIẾT ĐỐI TƯỢNG
                val selectedObject by mapViewModel.selectedObject.collectAsState()
                val sheetState = rememberModalBottomSheetState()

                if (selectedObject != null) {
                    ModalBottomSheet(
                        onDismissRequest = { mapViewModel.clearSelectedObject() },
                        sheetState = sheetState,
                        containerColor = Color.White
                    ) {
                        InfoDetailContent(
                            data = selectedObject!!,
                            userLocation = lastUserLocation,
                            onNavigate = { point, name ->
                                mapViewModel.navigateToDestination(point, name, lastUserLocation)
                            }
                        )
                    }
                }

            }
            // --- LỚP PHỦ LÀM MỜ TOÀN MÀN HÌNH KHI ĐÈ SOS ---

        }
    }
}
@Composable
fun InfoDetailContent(
    data: Any,
    userLocation: Point?,
    onNavigate: (Point, String) -> Unit
) {
    // Thêm dòng này để xem log trong Logcat
    LaunchedEffect(data) {
        println("DEBUG_DATA: $data")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        when (data) {
            is com.example.canhbao.data.model.SuCoMapDto -> {
                // Logic phân loại mức độ nguy hiểm
                val (levelText, advice, color) = when (data.mucDoNghiemTrong?.uppercase()) {
                    "HIGH" -> Triple("CAO", "Không nên tiến đến gần!", Color.Red)
                    "MEDIUM" -> Triple("TRUNG BÌNH", "Hạn chế khi lưu thông qua khu vực.", Color(0xFFFFA000))
                    "LOW" -> Triple("THẤP", "Cảnh giác khi lưu thông.", Color(0xFF2E7D32))
                    else -> Triple("KHÔNG XÁC ĐỊNH", "Cẩn thận khi di chuyển.", Color.Gray)
                }

                // Tên loại sự cố - Làm to hơn (HeadlineSmall)
                Text(
                    text = (data.tenLoai ?: "SỰ CỐ").uppercase(),
                    style = MaterialTheme.typography.headlineSmall, // Font to và đậm hơn
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Chip hiển thị mức độ nguy hiểm
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = color, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Mức độ $levelText: $advice",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hình ảnh hiện trường
                data.hinhAnhUrl?.let { url ->
                    val fullUrl = if (url.startsWith("http")) url else "${AppConfig.HTTP_BASE_URL}$url"
                    Card(
                        modifier = Modifier.fillMaxWidth().height(220.dp).padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        AsyncImage(
                            model = fullUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Thông tin chi tiết sử dụng InfoRow đã định nghĩa
                InfoRow(Icons.Default.Place, "Địa chỉ hiện trường", data.diaChi ?: "${data.tenDuong ?: "Đang cập nhật"}")
                InfoRow(Icons.Default.Person, "Người báo cáo", data.tenNguoiBao ?: "Hệ thống tự động")
                InfoRow(Icons.Default.Description, "Mô tả chi tiết", data.moTa ?: "Không có mô tả bổ sung.")

                NavigationButton(color) {
                    onNavigate(Point.fromLngLat(data.kinhDo, data.viDo), data.tenLoai ?: "Sự cố")
                }
            }

            is com.example.canhbao.data.model.TruSoMapDto -> {
                Text(
                    text = data.tenTruSo.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0052CC)
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(Icons.Default.LocationOn, "Vị trí trụ sở", "Tọa độ GPS: ${data.viDo}, ${data.kinhDo}")

                NavigationButton(Color(0xFF0052CC)) {
                    onNavigate(Point.fromLngLat(data.kinhDo, data.viDo), data.tenTruSo)
                }
            }
            is com.example.canhbao.data.model.CameraMapDto -> {
                // Tên Camera
                Text(
                    text = data.tenCamera,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
                Surface(
                    color = Color(0xFF1976D2).copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Tọa độ: ${data.viDo}, ${data.kinhDo}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 1. PHẦN HIỂN THỊ VIDEO HOẶC ẢNH CAMERA
                if (!data.videoUrl.isNullOrEmpty()) {
                    Text("Trực tiếp từ Camera:", style = MaterialTheme.typography.labelMedium)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        // Trình phát Video đơn giản
                        AndroidView(
                            factory = { ctx ->
                                android.widget.VideoView(ctx).apply {
                                    val fullUrl = if (data.videoUrl.startsWith("http")) data.videoUrl
                                    else "http://192.168.1.2:8080${data.videoUrl}"
                                    setVideoPath(fullUrl)
                                    val mc = android.widget.MediaController(ctx)
                                    mc.setAnchorView(this)
                                    setMediaController(mc)
                                    setOnPreparedListener { start() }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else if (!data.anhCamera.isNullOrEmpty()) {
                    // Nếu không có video thì hiện ảnh đại diện camera
                    AsyncImage(
                        model = if (data.anhCamera.startsWith("http")) data.anhCamera
                        else "${AppConfig.HTTP_BASE_URL}${data.anhCamera}",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. MÔ TẢ CAMERA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = data.moTa ?: "Không có mô tả chi tiết cho camera này.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 3. NÚT CHỈ ĐƯỜNG ĐẾN CAMERA
                Button(
                    onClick = { onNavigate(Point.fromLngLat(data.kinhDo, data.viDo), data.tenCamera) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Chỉ đường đến đây")
                }
            }

        }
    }
}
// Hàm phụ để vẽ từng dòng thông tin cho đẹp
@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun DetailItem(title: String, name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color)
            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NavigationButton(color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Directions, null)
        Spacer(Modifier.width(8.dp))
        Text("DẪN ĐƯỜNG ĐẾN ĐÂY")
    }
}
/* --- Các UI thành phần khác SearchUI, RouteInfoUI giữ nguyên nội dung --- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUI(searchText: String, onValueChange: (String) -> Unit, suggestions: List<String>, onSelected: (String) -> Unit, onMyLocationClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        ElevatedCard(shape = RoundedCornerShape(28.dp), elevation = CardDefaults.elevatedCardElevation(6.dp)) {
            TextField(
                value = searchText, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm địa điểm...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        // Khi đang gõ: Hiện nút X xóa chữ
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(Icons.Default.Clear, "Xóa")
                        }
                    } else {
                        // Khi trống: Hiện nút Zoom vị trí
                        IconButton(onClick = onMyLocationClick) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Vị trí của tôi",
                                tint = Color(0xFF4285F4) // Màu xanh đặc trưng
                            )
                        }
                    }
                }            )
        }
        if (suggestions.isNotEmpty()) {
            Surface(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp, color = Color.White) {
                Column {
                    suggestions.forEach { suggestion ->
                        Text(text = suggestion, modifier = Modifier.fillMaxWidth().clickable { onSelected(suggestion) }.padding(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun RouteInfoUI(mode: String, icon: androidx.compose.ui.graphics.vector.ImageVector, duration: String, distance: String, onBack: () -> Unit, onStartNav: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        ElevatedCard(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).statusBarsPadding(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(duration, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 20.sp)
                        Text(distance, color = Color.Gray, fontSize = 14.sp)
                    }
                    Icon(icon, contentDescription = null, tint = Color(0xFF4285F4), modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStartNav, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))) {
                    Text("BẮT ĐẦU", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}




// Hàm này nên giống hệt nhau ở cả 2 file hoặc tốt nhất là để trong 1 file Utils
fun createTruSoMarkerBitmap(context: Context): Bitmap {
    val width = 120
    val height = 120
    val cornerRadius = 8f
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

// --- BƯỚC 1: VẼ NỀN XANH ---
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.rgb(0, 82, 204)
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

    // --- BƯỚC 2: VẼ VIỀN TRẮNG MỎNG ---
    paint.style = Paint.Style.STROKE
    // Giảm xuống 2f hoặc 1.5f để viền trông thanh mảnh hơn
    paint.strokeWidth = 2f
    paint.color = android.graphics.Color.WHITE
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

    // 3. Vẽ Icon của BẠN
    paint.style = Paint.Style.FILL
    val iconSize = 100
    val iconLeft = (width - iconSize) / 2
    val iconTop = (height - iconSize) / 2

    // QUAN TRỌNG: Dùng chính xác package name để tránh lấy nhầm hệ thống
    val drawable = ContextCompat.getDrawable(context, com.example.canhbao.R.drawable.tru_so)

    drawable?.let {
        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        // Nếu ảnh tru_so của bạn đã có màu, hãy bỏ dòng setTint này đi
        // it.setTint(android.graphics.Color.WHITE)
        it.draw(canvas)
    }

    return output
}
fun createCameraIcon(context: Context): Bitmap {
    val width = 120
    val height = 120
    val cornerRadius = 8f
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

// --- BƯỚC 1: VẼ NỀN ĐEN ---
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.rgb(0,0,0)
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

    // --- BƯỚC 2: VẼ VIỀN TRẮNG MỎNG ---
    paint.style = Paint.Style.STROKE
    // Giảm xuống 2f hoặc 1.5f để viền trông thanh mảnh hơn
    paint.strokeWidth = 2f
    paint.color = android.graphics.Color.WHITE
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)


    paint.style = Paint.Style.FILL
    val iconSize = 100
    val iconLeft = (width - iconSize) / 2
    val iconTop = (height - iconSize) / 2

    val drawable = ContextCompat.getDrawable(context, com.example.canhbao.R.drawable.camera)

    drawable?.let {
        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        // Nếu ảnh tru_so của bạn đã có màu, hãy bỏ dòng setTint này đi
        // it.setTint(android.graphics.Color.WHITE)
        it.draw(canvas)
    }
    return output

}
/**
 * Tạo danh sách các điểm tọa độ để vẽ hình tròn xung quanh vị trí người dùng
 */
fun createCirclePoints(center: Point, radiusInMeters: Double): List<Point> {
    val points = mutableListOf<Point>()
    val numPoints = 64 // Độ mịn của hình tròn
    for (i in 0 until numPoints) {
        val angle = Math.PI * 2 * i / numPoints
        val latOffset = (radiusInMeters / 111320.0) * Math.cos(angle)
        val lonOffset = (radiusInMeters / (111320.0 * Math.cos(Math.toRadians(center.latitude())))) * Math.sin(angle)
        points.add(Point.fromLngLat(center.longitude() + lonOffset, center.latitude() + latOffset))
    }
    points.add(points[0]) // Đóng vòng tròn bằng cách nối lại điểm đầu
    return points
}

@Composable
fun DirectionHeaderUI(heading: Float) {
    // Chuẩn hóa mọi góc (âm hay dương) về dải 0..360 độ
    val normalizedHeading = (heading % 360 + 360) % 360

    val directionName = when {
        normalizedHeading >= 337.5 || normalizedHeading < 22.5 -> "Bắc"
        normalizedHeading in 22.5..67.5 -> "Đông Bắc"
        normalizedHeading in 67.5..112.5 -> "Đông"
        normalizedHeading in 112.5..157.5 -> "Đông Nam"
        normalizedHeading in 157.5..202.5 -> "Nam"
        normalizedHeading in 202.5..247.5 -> "Tây Nam"
        normalizedHeading in 247.5..292.5 -> "Tây"
        normalizedHeading in 292.5..337.5 -> "Tây Bắc"
        else -> "Bắc"
    }

    // Hiển thị (Thêm animation để chữ thay đổi mượt mà)
//    Surface(
//        modifier = Modifier
//            .padding(horizontal = 24.dp, vertical = 4.dp)
//            .fillMaxWidth(),
//        shape = RoundedCornerShape(16.dp),
//        color = Color.White.copy(alpha = 0.9f),
//        shadowElevation = 4.dp
//    ) {
//        Row(
//            modifier = Modifier.padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically,
//            horizontalArrangement = Arrangement.Center
//        ) {
//            // Icon xoay theo hướng thực tế của người dùng
//            Icon(
//                imageVector = Icons.Default.Navigation,
//                contentDescription = null,
//                tint = Color(0xFF4285F4),
//                modifier = Modifier
//                    .size(20.dp)
//                    .scale(0.8f) // Thu nhỏ icon mũi tên
//            )
//            Spacer(Modifier.width(8.dp))
//            Text(
//                text = "Bạn đang hướng về: ",
//                style = MaterialTheme.typography.bodySmall,
//                color = Color.DarkGray
//            )
//            Text(
//                text = directionName,
//                style = MaterialTheme.typography.bodySmall,
//                fontWeight = FontWeight.ExtraBold,
//                color = Color(0xFFD32F2F) // Màu đỏ để nổi bật hướng
//            )
//        }
//    }
}
@Composable
fun ActionCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}