package com.example.canhbao.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canhbao.data.model.TuiDto
import com.example.canhbao.viewmodel.QuaViewModel
import androidx.compose.foundation.shape.CircleShape


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuiScreen(viewModel: QuaViewModel, uid: String, onBack: () -> Unit) {
    // Tab lọc: TẤT CẢ, SAN_PHAM, VOUCHER
    val tabs = listOf("TẤT CẢ", "SAN_PHAM", "VOUCHER")
    var selectedTabIdx by remember { mutableIntStateOf(0) }

    val trafficBlue = Color(0xFF1976D2)
    val trafficOrange = Color(0xFFF57C00)
    // State cho Dialog Form Nhận quà
    var showForm by remember { mutableStateOf(false) }
    var selectedItemName by remember { mutableStateOf("") }

    // State cho các trường nhập liệu trong Form (để tĩnh nhưng vẫn nên có state)
    var hoTen by remember { mutableStateOf("") }
    var sdt by remember { mutableStateOf("") }
    var diaChi by remember { mutableStateOf("") }

    // Gọi load dữ liệu khi vào màn hình
    LaunchedEffect(Unit) {
        viewModel.loadTuiQua()
    }

    // Logic lọc danh sách dựa trên Tab
    val filteredList = if (tabs[selectedTabIdx] == "TẤT CẢ") {
        viewModel.listTuiQua
    } else {
        viewModel.listTuiQua.filter { it.loai == tabs[selectedTabIdx] }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( // Dùng CenterAligned để tiêu đề ở chính giữa
                title = {
                    Text(
                        "TÚI QUÀ CỦA TÔI",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = trafficBlue,
                    navigationIconContentColor = trafficBlue
                ),
                modifier = Modifier.border(0.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(0.dp))
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).background(Color(0xFFF8F9FA))) {
            // TabBar lọc loại quà phong cách hiện đại
            TabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = Color.White,
                contentColor = trafficBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIdx]),
                        color = trafficBlue,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIdx == index,
                        onClick = { selectedTabIdx = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIdx == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // 2. Hiển thị Loading hoặc Danh sách
            if (viewModel.isTuiLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Bạn chưa có vật phẩm nào trong mục này", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { item ->
                        TuiItemRow(item) {
                            selectedItemName = item.tenQua
                            showForm = true
                        }
                    }
                }
            }
        }

        // --- 3. Dialog Form Nhận Sản Phẩm (Tĩnh) ---
        if (showForm) {
            AlertDialog(
                onDismissRequest = { showForm = false },
                title = { Text("Thông tin nhận quà", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Vật phẩm: $selectedItemName", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)

                        OutlinedTextField(
                            value = hoTen,
                            onValueChange = { hoTen = it },
                            label = { Text("Họ và tên") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = sdt,
                            onValueChange = { sdt = it },
                            label = { Text("Số điện thoại") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = diaChi,
                            onValueChange = { diaChi = it },
                            label = { Text("Địa chỉ nhận hàng") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        Text("Ngày gửi yêu cầu: 21/01/2026", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Tạm thời để tĩnh, chỉ đóng dialog
                            showForm = false
                        }
                    ) { Text("Xác nhận gửi") }
                },
                dismissButton = {
                    TextButton(onClick = { showForm = false }) { Text("Hủy") }
                }
            )
        }
    }
}

@Composable
fun TuiItemRow(item: TuiDto, onNhanNgayClick: () -> Unit) {
    val isVoucher = item.loai == "VOUCHER"
    val isExpired = item.ngayKetThuc != null &&
            java.time.LocalDateTime.parse(item.ngayKetThuc)
                .isBefore(java.time.LocalDateTime.now())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon đại diện theo loại
            Surface(
                color = if (isVoucher) Color(0xFFE3F2FD) else Color(0xFFFFF3E0),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isVoucher) Icons.Default.ConfirmationNumber else Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = if (isVoucher) Color(0xFF1976D2) else Color(0xFFF57C00),
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.tenQua ?: "",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                    if (item.soLuong > 1) {
                        Text(
                            text = " x${item.soLuong}",
                            color = Color(0xFFF57C00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isVoucher && item.giaTriGiamPercent != null) {
                    Text(
                        "Giảm ${item.giaTriGiamPercent}% - Tối đa ${item.giaTriToiDa?.toInt()}đ",
                        color = Color(0xFF1976D2),
                        fontSize = 12.sp
                    )
                }
                if (isExpired) {
                    Text(
                        "Hết hạn",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    item.ngayKetThuc?.let {
                        Text(
                            "HSD: ${it.take(16).replace("T", " ")}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Nút bấm phong cách giao thông
            Button(
                onClick = { if (isVoucher) { /* dùng */ } else onNhanNgayClick() },
                enabled = !isExpired,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVoucher) Color(0xFF1976D2) else Color(0xFFF57C00)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (isVoucher) "Dùng ngay" else "Nhận ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}