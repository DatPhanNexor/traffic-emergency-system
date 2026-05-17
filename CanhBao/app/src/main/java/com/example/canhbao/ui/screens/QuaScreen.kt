package com.example.canhbao.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.canhbao.data.model.QuaDto
import com.example.canhbao.data.network.AppConfig
import com.example.canhbao.viewmodel.QuaViewModel
import android.util.Log
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuaScreen(
    viewModel: QuaViewModel,
    navController: NavController
) {
    val tabs = listOf("TẤT CẢ", "SAN_PHAM", "VOUCHER")
    val context = LocalContext.current
    val userPoints = viewModel.userProfile?.totalPoints ?: 0

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ĐỔI QUÀ TÍCH ĐIỂM",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.shadow(2.dp)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {

            // ===== HEADER =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1976D2), Color(0xFF42A5F5))
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            "Điểm tích lũy của bạn",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, null, tint = Color(0xFFFFD700))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                userPoints.toString(),
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Icon(
                        Icons.Default.CardGiftcard,
                        null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            // ===== TAB =====
            TabRow(
                selectedTabIndex = tabs.indexOf(viewModel.selectedTab),
                containerColor = Color.Transparent,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(
                            tabPositions[tabs.indexOf(viewModel.selectedTab)]
                        ),
                        color = Color(0xFF1976D2),
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEach { tab ->
                    val isSelected = viewModel.selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.filterQua(tab) },
                        text = {
                            Text(
                                if (tab == "SAN_PHAM") "SẢN PHẨM" else tab,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1976D2) else Color.Gray
                            )
                        }
                    )
                }
            }

            // ===== LIST =====
            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.filteredQua, key = { it.id }) { qua ->
                        ModernQuaItem(
                            qua = qua,
                            userPoints = userPoints,
                            onExchange = {
                                viewModel.thucHienDoiQua(qua) { _, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernQuaItem(
    qua: QuaDto,
    userPoints: Int,
    onExchange: () -> Unit
) {
    val isExpired = qua.ngayKetThuc?.let {
        try {
            val endTime = java.time.LocalDateTime.parse(it)
            endTime.isBefore(java.time.LocalDateTime.now())
        } catch (e: Exception) {
            false
        }
    } ?: false

    val isActive = qua.trangThai == "HOAT_DONG" && !isExpired
    val canExchange = userPoints >= qua.diem && isActive

    val imageUrl = "${AppConfig.HTTP_BASE_URL}/uploads/${qua.hinhAnh}"
    Log.d("QUA_IMAGE", "imageUrl = $imageUrl")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isActive) 1f else 0.5f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color.White else Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ===== IMAGE =====
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    onError = {
                        Log.e("QUA_IMAGE", "LOAD FAIL: $imageUrl")
                    },
                    onSuccess = {
                        Log.d("QUA_IMAGE", "LOAD OK: $imageUrl")
                    }
                )

                val label = when {
                    isExpired -> "HẾT HẠN"
                    qua.trangThai == "NGUNG" -> "TẠM NGƯNG"
                    else -> ""
                }

                if (label.isNotEmpty()) {
                    Text(
                        label,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(4.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // ===== CONTENT =====
            Column(modifier = Modifier.weight(1f)) {

                Text(
                    qua.ten,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (qua.loai == "VOUCHER") {
                    qua.giaTriGiamPercent?.let {
                        Text(
                            "Giảm $it% (Tối đa ${String.format("%,.0f", qua.giaTriToiDa ?: 0.0)}đ)",
                            color = Color(0xFF1976D2)
                        )
                    }
                }

                Text(
                    qua.moTa ?: "",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Text("${qua.diem} điểm")
            }

            // ===== BUTTON =====
            Button(
                onClick = onExchange,
                enabled = canExchange
            ) {
                Text(
                    when {
                        !isActive -> "Ngưng"
                        canExchange -> "Đổi"
                        else -> "Thiếu"
                    }
                )
            }
        }
    }
}