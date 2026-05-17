package com.example.canhbao.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canhbao.R
import com.example.canhbao.viewmodel.AuthViewModel
import com.example.canhbao.viewmodel.LoginUiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { viewModel.firebaseAuthWithGoogle(it) }
        } catch (e: ApiException) {
            Log.e("LoginError", "Mã lỗi Google: ${e.statusCode}")
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess?.invoke()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 1. Background Image làm mờ ---
        Image(
            painter = painterResource(id = R.drawable.login), // File trong res/drawable/login
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp), // Độ mờ tùy chỉnh
            contentScale = ContentScale.Crop
        )

        // Phủ một lớp màu tối nhẹ để chữ phía trên dễ đọc hơn
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

        // --- 2. Nội dung chính ---
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Tiêu đề phía trên cùng
            Text(
                text = "CỨU TRỢ GIAO THÔNG\nAN TOÀN ĐẾN MỌI NGƯỜI!",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Box(
                modifier = Modifier.weight(1f), // Đẩy Box đăng nhập ra giữa
                contentAlignment = Alignment.Center
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    // --- 3. Box đăng nhập nổi lên ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.95f), // Trắng hơi trong suốt nhẹ
                        shadowElevation = 12.dp // Tạo bóng đổ cho Box
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.sos), // Đảm bảo tên file là sos.png/jpg trong drawable
                                contentDescription = "SOS Logo",
                                modifier = Modifier
                                    .size(150.dp), // Bạn có thể chỉnh to nhỏ tùy ý (80.dp hoặc 120.dp)
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Nút đăng nhập Google tùy chỉnh
                            OutlinedButton(
                                onClick = {
                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken("282198564306-nap0kvk1i3ht02gl465i3dokjqcb1987.apps.googleusercontent.com")
                                        .requestEmail()
                                        .build()
                                    val client = GoogleSignIn.getClient(context, gso)
                                    client.signOut().addOnCompleteListener {
                                        launcher.launch(client.signInIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Icon Google (Bạn cần thêm icon này vào drawable)
                                    // Nếu chưa có, bạn có thể tạm thời bỏ Image này đi
                                    Icon(
                                        painter = painterResource(id = R.drawable.gg), // Bạn hãy thêm icon google vào đây
                                        contentDescription = "Google Icon",
                                        modifier = Modifier.size(50.dp),
                                        tint = Color.Unspecified // Giữ nguyên màu đa sắc của icon
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Đăng nhập Google",
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Một chút text nhỏ phía dưới cùng
            Text(
                text = "Phiên bản 2026.1.1",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
