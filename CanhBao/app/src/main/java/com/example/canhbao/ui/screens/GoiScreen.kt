package com.example.canhbao.ui.screens

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.canhbao.R
import com.example.canhbao.data.model.GoiDto
import com.example.canhbao.data.model.MuaGoiDto
import com.example.canhbao.viewmodel.GoiViewModel
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoiScreen(
    viewModel: GoiViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val dsGoi by viewModel.goiList.collectAsState()
    val myPackages by viewModel.myPackages.collectAsState()
    val socketError by viewModel.socketErrorMessage.collectAsState()

    var selectedGoiToBuy by remember { mutableStateOf<GoiDto?>(null) }
    var showQR by remember { mutableStateOf(false) }

    // Đồng bộ dữ liệu khi khởi chạy
    LaunchedEffect(Unit) {
        viewModel.fetchGoi()
        viewModel.fetchMyPackages()
        viewModel.connectSocket()
    }

    LaunchedEffect(socketError) {
        socketError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSocketError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "GÓI CỨU HỘ SOS",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Tiêu đề giới thiệu
                item {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            "Đặc quyền bảo vệ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            "An tâm trên mọi nẻo đường cùng trợ thủ SOS",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Danh sách gói tích hợp
                items(dsGoi, key = { it.id }) { goi ->
                    // Tìm xem gói này có đang được người dùng sở hữu (Active/Pending) không
                    val userPackage = myPackages.find { it.goiId == goi.id && it.trangThai != "CANCELLED" }

                    ModernGoiCard(
                        goi = goi,
                        userPkg = userPackage,
                        onBuyClick = {
                            selectedGoiToBuy = goi
                            showQR = true
                        },
                        // Trong GoiScreen.kt
                        onCancelClick = {
                            userPackage?.let { pkg ->
                                viewModel.huyGoi(pkg.id) { success, message -> // Thêm 'message' ở đây
                                    if (success) {
                                        Toast.makeText(context, "Đã hủy yêu cầu thành công", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // Tận dụng cái message trả về từ Backend để hiện thông báo lỗi thực tế
                                        Toast.makeText(context, message ?: "Không thể hủy gói", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    Text(
                        "* Lưu ý: Bạn chỉ có thể sở hữu 1 gói dịch vụ tại một thời điểm.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
            }
        }
    }

    // Dialog quét mã QR thanh toán
    if (showQR && selectedGoiToBuy != null) {
        AlertDialog(
            onDismissRequest = { showQR = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.muaGoi(selectedGoiToBuy!!.id){ success, message ->
                            if (success) {
                                Toast.makeText(context, "Hệ thống đang xử lý giao dịch!", Toast.LENGTH_SHORT).show()
                                showQR = false
                            } else {
                                Toast.makeText(context, message ?: "Lỗi đăng ký", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Tôi đã chuyển khoản", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQR = false }) { Text("Hủy bỏ") }
            },
            title = { Text("Xác nhận thanh toán", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.qr_goi),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp).padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(selectedGoiToBuy!!.ten, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(
                        "${String.format("%,d", selectedGoiToBuy!!.gia.toLong())} VNĐ",
                        color = Color.Red,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Vui lòng chuyển khoản đúng số tiền trên để gói được tự động kích hoạt.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }
            }
        )
    }
}

@Composable
fun ModernGoiCard(
    goi: GoiDto,
    userPkg: MuaGoiDto?,
    onBuyClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isOwned = userPkg != null
    val isPending = userPkg?.trangThai == "PENDING"

    // Màu sắc chủ đạo theo trạng thái
    val statusColor = when {
        isPending -> Color(0xFFFBC02D) // Vàng - Đang chờ
        isOwned -> Color(0xFF4CAF50)   // Xanh lá - Đang dùng
        else -> Color(0xFF1976D2)      // Xanh dương - Khám phá
    }

    // Đếm ngược cho gói Pending
    var timeLeft by remember { mutableLongStateOf(60L) }
    LaunchedEffect(isPending, userPkg?.ngayMua) {
        if (isPending && userPkg?.ngayMua != null) {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val purchaseTime = LocalDateTime.parse(userPkg.ngayMua, formatter)
            while (true) {
                val now = LocalDateTime.now()
                val diff = Duration.between(purchaseTime, now).seconds
                timeLeft = (60 - diff).coerceAtLeast(0)
                if (timeLeft <= 0) break
                delay(1000L)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isOwned) 8.dp else 2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (isOwned) BorderStroke(2.dp, statusColor) else null
    ) {
        Column {
            // Thanh màu trạng thái trên cùng
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).background(statusColor))

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (isOwned) {
                            Surface(
                                color = statusColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isPending) "CHỜ THANH TOÁN (${timeLeft}s)" else "GÓI ĐANG HIỆU LỰC",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            text = goi.ten,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2C3E50)
                        )
                    }

                    // Giá gói
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format("%,d", goi.gia?.toLong() ?: 0L)}đ",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor
                        )
                        Text(
                            text = "${goi.thoiHan} ngày",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = Color.LightGray)

                // Danh sách ưu đãi
                BenefitItem(Icons.Default.DirectionsCar, "Cứu hộ miễn phí: ${goi.khoangCachMienPhi}km")
                BenefitItem(Icons.Default.VerifiedUser, goi.uuDai)
                BenefitItem(Icons.Default.Update, "Hỗ trợ ưu tiên 24/7")

                if (isOwned && !isPending) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventAvailable, null, Modifier.size(14.dp), tint = Color.Gray)
                        Text(
                            text = " Hạn dùng: ${userPkg?.ngayHetHan?.replace("T", " ")}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Nút hành động
                if (isPending) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("HỦY ĐĂNG KÝ", fontWeight = FontWeight.Bold)
                    }
                } else if (!isOwned) {
                    Button(
                        onClick = onBuyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = statusColor)
                    ) {
                        Text("MUA NGAY", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                } else {
                    // Trạng thái đã kích hoạt
                    Button(
                        onClick = { /* Gói đang dùng không cho mua thêm */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECEFF1))
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("ĐANG SỞ HỮU", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BenefitItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFF546E7A)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color(0xFF455A64),
            fontWeight = FontWeight.Medium
        )
    }
}