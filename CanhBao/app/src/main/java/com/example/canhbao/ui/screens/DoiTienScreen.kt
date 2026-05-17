package com.example.canhbao.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.canhbao.data.model.DoiTienDtoRealtime
import com.example.canhbao.viewmodel.DoiTienViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoiTienScreen(
    navController: NavController,
    uid: String?,
    viewModel: DoiTienViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val user = viewModel.userDetail
    val currentPoints = user?.totalPoints ?: 0
    val snackbarHostState = remember { SnackbarHostState() }

    // State cho UI
    var showDonationDialog by remember { mutableStateOf(false) }
    var showExchangeDialog by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    // Picker chọn ngày (Dùng chung cho cả 3 chế độ để lấy mốc thời gian)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
        },
        viewModel.selectedDate.year,
        viewModel.selectedDate.monthValue - 1,
        viewModel.selectedDate.dayOfMonth
    )

    LaunchedEffect(uid) {
        if (uid != null) viewModel.init(uid)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("QUY ĐỔI & THIỆN NGUYỆN", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF1F3F6))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- CARD ĐIỂM CÁ NHÂN ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Điểm tích lũy khả dụng", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(
                        text = currentPoints.toString(),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    if (viewModel.isProcessing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle("Dịch vụ quy đổi")

            ActionRewardCard(
                title = "Đổi Tiền Mặt",
                subtitle = "100 điểm = 10.000đ",
                icon = Icons.Default.MonetizationOn,
                color = Color(0xFF1976D2),
                enabled = currentPoints >= 100 && !viewModel.isProcessing,
                onClick = { showExchangeDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionRewardCard(
                title = "Quỹ Giao Thông",
                subtitle = "10 điểm = 1.000đ ủng hộ",
                icon = Icons.Default.Favorite,
                color = Color(0xFFD32F2F),
                enabled = currentPoints >= 10 && !viewModel.isProcessing,
                onClick = { showDonationDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- BẢNG VINH DANH ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bảng vinh danh 🏆", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(
                                "Tổng: ${String.format("%,d", viewModel.totalFilteredValue)}đ",
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Menu chọn lọc (Ngày/Tháng/Năm)
                        Box {
                            TextButton(onClick = { expandedMenu = true }) {
                                val label = when(viewModel.filterMode) {
                                    com.example.canhbao.viewmodel.FilterMode.DAY -> "Ngày"
                                    com.example.canhbao.viewmodel.FilterMode.MONTH -> "Tháng"
                                    com.example.canhbao.viewmodel.FilterMode.YEAR -> "Năm"
                                }
                                Text(label, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1976D2))
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color(0xFF1976D2))
                            }
                            DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Theo Ngày") },
                                    onClick = { viewModel.filterMode = com.example.canhbao.viewmodel.FilterMode.DAY; expandedMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Theo Tháng") },
                                    onClick = { viewModel.filterMode = com.example.canhbao.viewmodel.FilterMode.MONTH; expandedMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Theo Năm") },
                                    onClick = { viewModel.filterMode = com.example.canhbao.viewmodel.FilterMode.YEAR; expandedMenu = false }
                                )
                            }
                        }
                    }

                    // Nút hiển thị và chọn mốc thời gian cụ thể
                    // --- THAY THẾ PHẦN OutlinedCard TRONG SCREEN CỦA BẠN BẰNG ĐOẠN NÀY ---

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Nút Lùi
                        IconButton(onClick = { viewModel.prevPeriod() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Trước", tint = Color(0xFF1976D2))
                        }

                        // Hiển thị thời gian hiện tại - Bấm vào vẫn hiện Lịch nếu muốn chọn xa
                        Surface(
                            onClick = { datePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8F9FA),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                val timeText = when(viewModel.filterMode) {
                                    com.example.canhbao.viewmodel.FilterMode.DAY -> viewModel.selectedDate.format(dateFormatter)
                                    com.example.canhbao.viewmodel.FilterMode.MONTH -> "Tháng " + viewModel.selectedDate.format(DateTimeFormatter.ofPattern("MM / yyyy"))
                                    com.example.canhbao.viewmodel.FilterMode.YEAR -> "Năm " + viewModel.selectedDate.year.toString()
                                }
                                Text(timeText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }

                        // Nút Tiến
                        IconButton(onClick = { viewModel.nextPeriod() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Sau", tint = Color(0xFF1976D2))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                    // Hiển thị danh sách từ ViewModel
                    val filteredList = viewModel.filteredVinhDanh
                    if (filteredList.isEmpty()) {
                        Text("Không có dữ liệu đóng góp", modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = Color.Gray)
                    } else {
                        filteredList.forEach { item ->
                            VinhDanhItem(item)
                        }
                    }
                }
            }
        }
    }

    // --- Các Dialog chọn điểm ---
    if (showExchangeDialog) {
        PointSelectionDialog(
            title = "Rút tiền mặt",
            minPoints = 100f,
            maxPoints = currentPoints.toFloat(),
            onDismiss = { showExchangeDialog = false },
            onConfirm = { points ->
                uid?.let { viewModel.thucHienGiaoDich(it, points, "TIEN_MAT") }
                showExchangeDialog = false
            }
        )
    }

    if (showDonationDialog) {
        PointSelectionDialog(
            title = "Quyên góp thiện nguyện",
            minPoints = 10f,
            maxPoints = currentPoints.toFloat(),
            onDismiss = { showDonationDialog = false },
            onConfirm = { points ->
                uid?.let { viewModel.thucHienGiaoDich(it, points, "QUYEN_GOP") }
                showDonationDialog = false
            }
        )
    }
}

// Các hàm ActionRewardCard, VinhDanhItem, SectionTitle, PointSelectionDialog giữ nguyên như code trước của bạn.

@Composable
fun ActionRewardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(color.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) {
                Text("Đổi", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VinhDanhItem(item: DoiTienDtoRealtime) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEEEEEE)), contentAlignment = Alignment.Center) {
            Text(item.userId?.take(1)?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.userId ?: "Ẩn danh", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(item.ngayDoi?.take(16)?.replace("T", " ") ?: "", fontSize = 10.sp, color = Color.Gray)
        }
        Text("+${String.format("%,d", item.giaTri ?: 0)}đ", color = Color(0xFF388E3C), fontWeight = FontWeight.Black)
    }
    Divider(color = Color(0xFFF5F5F5))
}

@Composable
fun SectionTitle(title: String) {
    Text(title, Modifier.fillMaxWidth().padding(bottom = 8.dp), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

@Composable
fun PointSelectionDialog(
    title: String,
    minPoints: Float,
    maxPoints: Float,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableStateOf(minPoints) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${sliderValue.toInt()} điểm", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF1976D2))
                Text("= ${String.format("%,d", sliderValue.toInt() * 100)}đ", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = minPoints..maxPoints.coerceAtLeast(minPoints)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(sliderValue.toInt()) }) { Text("Xác nhận") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}