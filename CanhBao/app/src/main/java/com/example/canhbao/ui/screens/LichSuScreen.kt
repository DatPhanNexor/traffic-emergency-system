package com.example.canhbao.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.canhbao.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.canhbao.data.model.HoaDonDto
import com.example.canhbao.data.model.LichSuDto
import com.example.canhbao.data.model.TuiDto
import com.example.canhbao.viewmodel.LichSuUiState
import com.example.canhbao.viewmodel.LichSuViewModel
import androidx.compose.ui.text.style.TextOverflow
import com.example.canhbao.data.network.AppConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LichSuScreen(
    navController: NavController,
    viewModel: LichSuViewModel
) {
    val trafficBlue = Color(0xFF1976D2)
    val sosRed = Color(0xFFD32F2F)

    // State quản lý UI
    var tempSelectedInvoice by remember { mutableStateOf<HoaDonDto?>(null) }
    var selectedMainTab by remember { mutableIntStateOf(0) }
    val mainTabs = listOf("SỰ CỐ", "SOS")
    var selectedStatusTab by remember { mutableIntStateOf(0) }
    val statusFilters = listOf("Chờ tiếp nhận", "Đang xử lý", "Đã xong", "Đã hủy")
    var showPayDialog by remember { mutableStateOf(false) }
    var selectedVoucher by remember { mutableStateOf<TuiDto?>(null) }

    // 1. CẬP NHẬT: Gọi fetchHistory chỉ với uid (Bỏ maThietBi)
    LaunchedEffect(Unit) {
        viewModel.fetchHistory()
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Column(modifier = Modifier.background(Color.White)) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text("LỊCH SỬ HOẠT ĐỘNG", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = trafficBlue)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                    )

                    TabRow(
                        selectedTabIndex = selectedMainTab,
                        containerColor = Color.White,
                        contentColor = if (selectedMainTab == 1) sosRed else trafficBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedMainTab]),
                                color = if (selectedMainTab == 1) sosRed else trafficBlue,
                                height = 3.dp
                            )
                        }
                    ) {
                        mainTabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedMainTab == index,
                                onClick = { selectedMainTab = index },
                                text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                            )
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedStatusTab,
                        edgePadding = 12.dp,
                        containerColor = Color(0xFFF8F9FA),
                        divider = {},
                        indicator = {}
                    ) {
                        statusFilters.forEachIndexed { index, label ->
                            val isSelected = selectedStatusTab == index
                            val activeColor = if (selectedMainTab == 1) sosRed else trafficBlue
                            Tab(
                                selected = isSelected,
                                onClick = { selectedStatusTab = index },
                                text = {
                                    Surface(
                                        color = if (isSelected) activeColor else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp),
                                        border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray),
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        Text(
                                            label,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Color.Gray
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))) {
            when (val state = viewModel.uiState) {
                is LichSuUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = trafficBlue)
                is LichSuUiState.Success -> {
                    val filteredData = state.data.filter { item ->
                        val matchesType = if (selectedMainTab == 0) item.loai == "SU_CO" else item.loai == "SOS"
                        val matchesStatus = when (selectedStatusTab) {
                            0 -> item.trangThaiXuLy == "CHO_XU_LY" || item.trangThaiXuLy == "PENDING"
                            1 -> item.trangThaiXuLy == "DANG_XU_LY" || item.trangThaiXuLy == "PROCESSING"
                            2 -> item.trangThaiXuLy == "HOAN_THANH" || item.trangThaiXuLy == "COMPLETED"
                            3 -> item.trangThaiXuLy == "HUY_BO" || item.trangThaiXuLy == "CANCELLED"
                            else -> true
                        }
                        matchesType && matchesStatus
                    }

                    if (filteredData.isEmpty()) {
                        EmptyStateUI(Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredData, key = { it.id }) { item ->
                                HistoryItemUI(
                                    item = item,
                                    pendingInvoice = viewModel.pendingInvoicesMap[item.id],
                                    isPlaying = viewModel.currentlyPlayingId == item.id,
                                    onPlayAudio = { url -> viewModel.playRecording(url, item.id) },
                                    // 2. CẬP NHẬT: Bỏ maThietBi trong cancelOrder
                                    onCancelClick = { viewModel.cancelOrder(item) },
                                    onPayClick = {
                                        tempSelectedInvoice = viewModel.pendingInvoicesMap[item.id]
                                        showPayDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                // 3. CẬP NHẬT: Bỏ maThietBi trong onRetry
                is LichSuUiState.Error -> ErrorUI(state.message, Modifier.align(Alignment.Center)) {
                    viewModel.fetchHistory()
                }
            }
        }
    }

    // --- DIALOG THANH TOÁN ---
    if (showPayDialog && tempSelectedInvoice != null) {
        val inv = tempSelectedInvoice!!
        val giaGoc = inv.thanhTien
        var soTienGiam = 0.0
        selectedVoucher?.let { voucher ->
            val phanTram = (voucher.giaTriGiamPercent ?: 0) / 100.0
            soTienGiam = giaGoc * phanTram
            voucher.giaTriToiDa?.let { if (soTienGiam > it) soTienGiam = it }
        }
        val tongThanhToanHienTai = (giaGoc - soTienGiam).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showPayDialog = false },
            confirmButton = {
                Button(onClick = {
                    // 4. CẬP NHẬT: Bỏ maThietBi trong confirmPayment
                        viewModel.confirmPayment(
                            inv.id,
                            selectedVoucher?.quaId,
                            inv.sosId
                        )

                    showPayDialog = false
                    selectedVoucher = null
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                    Text("Tôi đã chuyển khoản")
                }
            },
            dismissButton = { TextButton(onClick = { showPayDialog = false }) { Text("Đóng") } },
            title = { Text("Thanh toán cứu hộ", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Quét mã QR để thanh toán", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.qr_goi),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Voucher có thể dùng", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

                    viewModel.listTuiQua.filter { it.loai == "VOUCHER" && (it.soLuong ?: 0) > 0 }.forEach { voucher ->
                        val isSelected = selectedVoucher?.quaId == voucher.quaId
                        OutlinedCard(
                            onClick = { selectedVoucher = if (isSelected) null else voucher },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFFBC02D) else Color(0xFFE0E0E0))
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFE082)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ConfirmationNumber, null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(voucher.tenQua, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Số lượng: ${voucher.soLuong}", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                }
                                RadioButton(selected = isSelected, onClick = null)
                            }
                        }
                    }

                    Divider(Modifier.padding(vertical = 12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tổng thanh toán:", fontWeight = FontWeight.Bold)
                        Text("${String.format("%,d", tongThanhToanHienTai.toLong())} VNĐ", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        )
    }
}

// --- GIỮ NGUYÊN HistoryItemUI, EmptyStateUI, ErrorUI NHƯ TRƯỚC ---
// (Chỉ cần đảm bảo onCancelClick và onPayClick truyền từ Screen vào là đúng)

@Composable
fun HistoryItemUI(
    item: LichSuDto,
    pendingInvoice: HoaDonDto?,
    isPlaying: Boolean,
    onPlayAudio: (String) -> Unit,
    onCancelClick: () -> Unit,
    onPayClick: () -> Unit
) {
    val isSOS = item.loai == "SOS"
    val activeColor = if (isSOS) Color(0xFFD32F2F) else Color(0xFF1976D2)
    val containerColor = if (isSOS) Color(0xFFFFEBEE) else Color(0xFFE3F2FD)

    val safeTenLoai = item.tieuDe ?: (if (isSOS) "Yêu cầu cứu hộ" else "Sự cố giao thông")
    val safeTrangThai = item.trangThaiXuLy ?: "PENDING"
    val safeTrangThaiDuyet = item.trangThaiDuyet ?: ""
    val locationText = item.diaChi ?: "Không rõ vị trí"

    val rawTime = item.thoiGian ?: ""
    val displayDate = try { rawTime.substringBefore("T").split("-").reversed().joinToString("/") } catch (e: Exception) { "" }
    val displayTime = try { rawTime.substringAfter("T").take(5) } catch (e: Exception) { "" }

    // Logic kiểm tra trạng thái
    val isHoanThanh = safeTrangThai == "HOAN_THANH" || safeTrangThai == "COMPLETED"
    val hoaDonDaXong = item.hoaDon
    val canCancel = if (isSOS) {
        safeTrangThai == "CHO_XU_LY" || safeTrangThai == "PENDING"
    } else {
        (safeTrangThai == "CHO_XU_LY" || safeTrangThai == "PENDING") && safeTrangThaiDuyet == "AI_APPROVED"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1. PHẦN THÔNG TIN CHUNG (Ảnh + Text)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(85.dp).clip(RoundedCornerShape(8.dp)).background(containerColor)) {
                    if (item.hinhAnhUrl.isNullOrEmpty()) {
                        Icon(imageVector = if (isSOS) Icons.Default.Warning else Icons.Default.Build, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(32.dp), tint = activeColor)
                    } else {
                        AsyncImage(model = "${AppConfig.HTTP_BASE_URL}${item.hinhAnhUrl}", contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = safeTenLoai, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = activeColor)
                    if (!item.moTa.isNullOrEmpty()) Text(text = item.moTa!!, fontSize = 13.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = activeColor)
                        Text(text = " $locationText", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, Modifier.size(14.dp), tint = Color.Gray)
                        Text(text = " $displayDate", fontSize = 11.sp, color = Color.Gray)
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Default.Schedule, null, Modifier.size(14.dp), tint = activeColor)
                        Text(text = " $displayTime", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = activeColor)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. HÀNG ĐIỀU KHIỂN (Ghi âm bên trái, Nhóm Action bên phải)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NHÓM TRÁI: Chỉ có ghi âm
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (isSOS && !item.ghiAmUrl.isNullOrEmpty()) {
                        Button(
                            onClick = { onPlayAudio("${AppConfig.HTTP_BASE_URL}${item.ghiAmUrl}") },
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color.Black else Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                            Text(if (isPlaying) " Dừng" else " Ghi âm", fontSize = 12.sp)
                        }
                    }
                }

                // NHÓM PHẢI: Hủy, Thanh toán, Tổng tiền
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // Nút Hủy
                    if (canCancel) {
                        OutlinedButton(
                            onClick = onCancelClick,
                            modifier = Modifier.height(36.dp).padding(end = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Text("Hủy", fontSize = 12.sp)
                        }
                    }

                    // Nút Thanh toán
                    if (pendingInvoice != null && pendingInvoice.trangThai == "PENDING") {
                        Button(
                            onClick = onPayClick,
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Payment, null, Modifier.size(16.dp))
                            Text(" Thanh toán", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Tổng tiền (Khi đã hoàn thành)
                    if (isHoanThanh && hoaDonDaXong != null) {
                        Surface(
                            color = Color(0xFFF1F8E9),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tổng: ", fontSize = 11.sp, color = Color.DarkGray)
                                Text(
                                    "${String.format("%,d", (hoaDonDaXong.tongThanhToan ?: hoaDonDaXong.thanhTien).toLong())}đ",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable fun EmptyStateUI(modifier: Modifier) { Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.DateRange, null, tint = Color.LightGray, modifier = Modifier.size(64.dp)); Text("Không có dữ liệu", color = Color.Gray) } }
@Composable fun ErrorUI(message: String, modifier: Modifier, onRetry: () -> Unit) { Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { Text("Lỗi: $message", color = Color.Red); Button(onClick = onRetry) { Text("Thử lại") } } }