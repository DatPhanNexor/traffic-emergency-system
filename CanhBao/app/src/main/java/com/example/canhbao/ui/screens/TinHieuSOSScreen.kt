package com.example.canhbao.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.canhbao.viewmodel.TinHieuSOSViewModel
import java.io.File
import com.google.firebase.auth.FirebaseAuth
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TinHieuSOSScreen(
    navController: NavController,
    viewModel: TinHieuSOSViewModel,
    lat: Double,
    lng: Double,
    userId: String
) {
    val context = LocalContext.current
    val step by viewModel.currentStep.collectAsState()

    // Màu chủ đạo cho SOS
    val sosRed = Color(0xFFD32F2F)
    val lightRed = Color(0xFFFFEBEE)

    // Quản lý ghi âm
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    // Launcher xin quyền ghi âm
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Ứng dụng cần quyền Micro để ghi âm!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            if (lat == 0.0 || lng == 0.0) {
                Toast.makeText(context, "Lỗi tọa độ!", Toast.LENGTH_LONG).show()
            } else {
                // LẤY TOKEN MỚI NHẤT TỪ FIREBASE
                val user = FirebaseAuth.getInstance().currentUser
                user?.getIdToken(false)?.addOnSuccessListener { result ->
                    val token = result.token
                    if (token != null) {
                        // Gọi ViewModel với Token thay vì userId
                        viewModel.guiSOS(context, lat, lng, bitmap) { success ->
                            if (success) {
                                Toast.makeText(context, "Đã gửi cứu hộ thành công!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Lỗi xác thực người dùng!", Toast.LENGTH_SHORT).show()
                    }
                }?.addOnFailureListener {
                    Toast.makeText(context, "Không thể lấy token bảo mật!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Ứng dụng cần quyền Camera để chụp ảnh cứu hộ!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
            recorder = null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "GỬI CỨU HỘ SOS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)) // Nền xám cực nhẹ cho sang
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Thanh tiến trình đẹp hơn
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tiến độ: $step/3", style = MaterialTheme.typography.labelLarge, color = sosRed)
                    Text(if(step == 3) "Bước cuối" else "Tiếp theo: Bước ${step + 1}", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = step / 3f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.LightGray, CircleShape),
                    color = sosRed,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nội dung thay đổi theo Step với hiệu ứng Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        1 -> {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = sosRed, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("BƯỚC 1: GHI ÂM", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "Ghi lại âm thanh hiện trường để cứu hộ hiểu rõ hơn",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(40.dp))

                            Box(contentAlignment = Alignment.Center) {
                                // Hiệu ứng vòng tròn lan tỏa khi ghi âm (giả lập)
                                if (isRecording) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(160.dp),
                                        color = sosRed,
                                        strokeWidth = 2.dp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@IconButton
                                        }

                                        if (!isRecording) {
                                            try {
                                                val file = File(context.cacheDir, "sos_audio.m4a")
                                                recorder = MediaRecorder().apply {
                                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                    setAudioSamplingRate(44100)
                                                    setAudioEncodingBitRate(96000)
                                                    setOutputFile(file.absolutePath)
                                                    prepare()
                                                    start()
                                                }
                                                viewModel.recordedFile = file
                                                isRecording = true
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Lỗi Micro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            try {
                                                recorder?.stop()
                                                recorder?.release()
                                                recorder = null
                                                isRecording = false
                                                viewModel.nextStep()
                                            } catch (e: Exception) {
                                                isRecording = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = if (isRecording) listOf(Color.Black, Color.DarkGray) else listOf(sosRed, Color(0xFFB71C1C))
                                            ),
                                            shape = CircleShape
                                        )
                                ) {
                                    Icon(
                                        if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                if (isRecording) "ĐANG GHI ÂM..." else "Chạm vào micro để bắt đầu",
                                color = if (isRecording) sosRed else Color.Gray,
                                fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                            TextButton(onClick = { viewModel.nextStep() }) {
                                Text("BỎ QUA GHI ÂM", color = Color.Gray)
                            }
                        }

                        2 -> {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = sosRed, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("BƯỚC 2: MÔ TẢ", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = viewModel.noteText,
                                onValueChange = { viewModel.noteText = it },
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                placeholder = { Text("Mô tả tình trạng hiện tại của bạn...") },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = sosRed,
                                    focusedLabelColor = sosRed,
                                    cursorColor = sosRed
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { viewModel.nextStep() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = sosRed),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("TIẾP THEO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        3 -> {
                            Icon(Icons.Default.NewReleases, contentDescription = null, tint = sosRed, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("BƯỚC 3: XÁC NHẬN", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Chụp ảnh hiện trường để gửi yêu cầu", color = Color.Gray)

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                        cameraLauncher.launch()
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(80.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = sosRed),
                                shape = RoundedCornerShape(20.dp),
                                enabled = !viewModel.isSubmitting
                            ) {
                                if (viewModel.isSubmitting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.CameraAlt, null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("CHỤP ẢNH & GỬI CỨU HỘ", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                }
                            }

                            if (viewModel.statusSOS.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    viewModel.statusSOS,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = sosRed
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Dòng text bảo mật/nhắc nhở phía dưới cùng
            Text(
                "Vị trí của bạn sẽ được gửi kèm theo yêu cầu này",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}