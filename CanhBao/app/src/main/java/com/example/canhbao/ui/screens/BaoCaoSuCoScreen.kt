
package com.example.canhbao.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.canhbao.data.network.AppConfig
import com.example.canhbao.viewmodel.BaoCaoSuCoViewModel
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaoCaoSuCoScreen(
    viewModel: BaoCaoSuCoViewModel = viewModel(),
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var expanded by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var moTa by remember { mutableStateOf("") }
    var showCamera by remember { mutableStateOf(false) }

    // --- GPS State ---
    var lat by remember { mutableStateOf<Double?>(null) }
    var lng by remember { mutableStateOf<Double?>(null) }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Quyền Camera & Vị trí
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) showCamera = true else Toast.makeText(context, "Cần quyền Camera", Toast.LENGTH_SHORT).show()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { it?.let { lat = it.latitude; lng = it.longitude } }
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { it?.let { lat = it.latitude; lng = it.longitude } }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- TỰ ĐỘNG CHUYỂN TRANG KHI THÀNH CÔNG ---
    // --- TỰ ĐỘNG CHUYỂN TRANG KHI THÀNH CÔNG ---
    LaunchedEffect(viewModel.uploadStatus) {
        if (viewModel.uploadStatus.contains("AI_APPROVED")) {

            Toast.makeText(context, "Gửi báo cáo thành công!", Toast.LENGTH_SHORT).show()

            // Chờ một chút để người dùng thấy Toast
            kotlinx.coroutines.delay(800)

            // SỬA TẠI ĐÂY: Thay "all_history" bằng "lich_su" cho khớp với NavGraph
            navController.navigate("lich_su") {
                popUpTo("bao_cao_su_co") { inclusive = true }
            }
        }
    }
    if (showCamera) {
        CameraXScreen(
            onImageCaptured = { bitmap ->
                capturedBitmap = bitmap
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ showCamera = false }, 300)
            },
            onError = { showCamera = false }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BÁO CÁO SỰ CỐ", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // Nút gửi báo cáo nằm ở dưới cùng cố định
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = Color.White) {
                Button(
                    onClick = {
                        if (capturedBitmap == null || lat == null || viewModel.selectedLoaiSuCo == null) {
                            Toast.makeText(context, "Thiếu ảnh, vị trí hoặc loại sự cố!", Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.guiBaoCao(capturedBitmap!!, moTa, lat!!, lng!!)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    enabled = !viewModel.uploadStatus.contains("Đang") // Tránh bấm nhiều lần
                ) {
                    if (viewModel.uploadStatus.contains("Đang")) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Send, null)
                        Spacer(Modifier.width(8.dp))
                        Text("GỬI BÁO CÁO NGAY", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // 1. Khung chụp ảnh
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(2.dp, if(capturedBitmap == null) Color.LightGray else Color(0xFFD32F2F), RoundedCornerShape(24.dp))
                    .clickable {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (granted) showCamera = true else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (capturedBitmap != null) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Badge Overlay
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.4f)))))
                    Text("Bấm để chụp lại", color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), fontWeight = FontWeight.Bold)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, null, Modifier.size(60.dp), tint = Color(0xFFD32F2F))
                        Spacer(Modifier.height(12.dp))
                        Text("Chụp ảnh hiện trường", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Yêu cầu hình ảnh rõ nét", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. Thông tin vị trí (Mới)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF1976D2))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Vị trí hiện tại", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (lat != null) "Tọa độ: $lat, $lng" else "Đang xác định vị trí...",
                            fontSize = 12.sp, color = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. Chọn loại sự cố
            Text("LOẠI SỰ CỐ", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Box {
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val selected = viewModel.selectedLoaiSuCo
                        if (selected != null) {
                            AsyncImage(
                                model = "${AppConfig.HTTP_BASE_URL}${selected.iconUrl}",
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(selected?.ten ?: "Nhấn để chọn loại sự cố...", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f).background(Color.White)) {
                    viewModel.listLoaiSuCo.forEach { loai ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(model = "${AppConfig.HTTP_BASE_URL}${loai.iconUrl}", contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(loai.ten, fontWeight = FontWeight.Medium)
                                }
                            },
                            onClick = { viewModel.onLoaiSuCoSelected(loai); expanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 4. Mô tả
            Text("MÔ TẢ CHI TIẾT", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = moTa,
                onValueChange = { moTa = it },
                placeholder = { Text("Mô tả thêm về tình trạng...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors( // SỬA Ở ĐÂY
                    focusedBorderColor = Color(0xFFD32F2F),
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                minLines = 4
            )

            // Thông báo upload
            if (viewModel.uploadStatus.isNotEmpty()) {
                Text(
                    text = viewModel.uploadStatus,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                    color = if (viewModel.uploadStatus.contains("thành công")) Color(0xFF388E3C) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}