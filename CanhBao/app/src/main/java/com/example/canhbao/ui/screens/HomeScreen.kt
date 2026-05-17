package com.example.canhbao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.canhbao.data.model.SuCoUserDto
import com.example.canhbao.data.network.BaoCaoSuCoRetrofit
import com.example.canhbao.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userEmail: String?,
    userPhotoUrl: String?,
    uid: String?,
    navController: NavController,
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onLogout: () -> Unit
) {

    LaunchedEffect(Unit) {
        viewModel.initUserData()
    }
    // State lưu dữ liệu người dùng từ Backend
    val userDetail = viewModel.userDetail
    val isLoading = viewModel.isLoading



    if (userEmail.isNullOrEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Text("Bạn chưa đăng nhập", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { navController.navigate("login") }) { Text("Đăng nhập ngay") }
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, null)
                    Spacer(Modifier.width(8.dp))
                    Text("ĐĂNG XUẤT", fontWeight = FontWeight.Bold)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                // --- PHẦN HEADER MỚI: ĐẸP HƠN, ĐẦY ĐỦ EMAIL ---
                item {
                    val hasPackage = userDetail?.tenGoi != null

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                            .background(
                                if (hasPackage) Color(0xFFFFFDE7) else Color.Transparent, // Nền vàng nhạt nếu là VIP
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- AVATAR VỚI VƯƠNG MIỆN XỊN ---
                        Box(contentAlignment = Alignment.TopCenter) {
                            AsyncImage(
                                model = userPhotoUrl ?: "https://via.placeholder.com/150",
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (hasPackage) 3.dp else 1.dp,
                                        color = if (hasPackage) Color(0xFFFFD700) else Color.LightGray,
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )

                            if (hasPackage) {
                                // Icon Vương miện nổi bật hơn
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Premium",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier
                                        .size(30.dp)
                                        .offset(y = (-8).dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Tên người dùng
                            Text(
                                text = userDetail?.name ?: "Người dùng",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color(0xFF212121)
                            )

                            // EMAIL ĐÃ QUAY TRỞ LẠI Ở ĐÂY
                            Text(
                                text = userDetail?.email ?: userEmail ?: "",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            if (hasPackage) {
                                // Nhãn gói thiết kế lại kiểu Gradient hoặc Badge
                                Surface(
                                    color = Color(0xFFFFD700),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Text(
                                        text = userDetail?.tenGoi?.uppercase() ?: "VIP",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
                // 2. Thẻ hiển thị ĐIỂM và SPAM
                item {
                    Spacer(Modifier.height(8.dp))
                    if (isLoading && userDetail == null) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        // Card này sẽ tự cập nhật khi Socket đổ dữ liệu vào ViewModel
                        UserStatsCard(
                            points = userDetail?.totalPoints ?: 0,
                            spam = userDetail?.spamCount ?: 0
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // 3. Quản lý hoạt động
                item {
                    Text("Quản lý hoạt động", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    HistoryCategoryCard(
                        title = "Túi đồ",
                        description = "Vật phẩm đã đổi",
                        icon = Icons.Default.Inventory, // Sử dụng icon hộp đồ hoặc kho hàng
                        onClick = { navController.navigate("tui_screen") }
                    )

                    Spacer(Modifier.height(12.dp))
                    HistoryCategoryCard(
                        title = "Lịch sử báo cáo",
                        description = "Sự cố và cứu trợ",
                        icon = Icons.Default.History,
                        onClick = { navController.navigate("lich_su") }
                    )
                    Spacer(Modifier.height(12.dp))
                    HistoryCategoryCard(
                        title = "Gói đặc quyền ",
                        description = "Trải nghiệm cứu trợ tốt nhất",
                        icon = Icons.Default.CardMembership,
                        onClick = { navController.navigate("goi_screen") }
                    )

                    Spacer(Modifier.height(12.dp))
                    HistoryCategoryCard(
                        title = "Cửa hàng đổi quà",
                        description = "Dùng điểm đổi quà và voucher",
                        icon = Icons.Default.CardGiftcard,
                        onClick = { navController.navigate("qua_screen") }
                    )
                    Spacer(Modifier.height(12.dp))

                    HistoryCategoryCard(
                        title = "Quy đổi tiền",
                        description = "Rút tiền, quyên góp thiện nguyện",
                        icon = Icons.Default.Money,
                        onClick = { navController.navigate("doitien_screen") }
                    )
                }
            }
        }
    }
}

@Composable
fun UserStatsCard(points: Int, spam: Int) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Điểm tích lũy", fontSize = 12.sp, color = Color.Gray)
                Text(
                    text = points.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32)
                )
            }

            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFEEEEEE)))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Báo cáo sai", fontSize = 12.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = spam.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCategoryCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Cho phép nhấn vào toàn bộ vùng
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 4.dp), // Tăng khoảng cách trên dưới cho thoáng
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon với màu sắc nhẹ nhàng hơn
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFF424242)
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold, // Giảm độ đậm một chút cho tinh tế
                    fontSize = 16.sp
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }

        // Đường kẻ mỏng ở dưới mỗi mục
        HorizontalDivider(
            modifier = Modifier.padding(start = 40.dp), // Thụt đầu dòng để thẳng hàng với Text
            thickness = 0.5.dp,
            color = Color(0xFFEEEEEE)
        )
    }
}