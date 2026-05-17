package com.example.canhbao.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.canhbao.data.network.BaoCaoSuCoRetrofit // Đảm bảo đúng path tới Retrofit của bạn
import com.example.canhbao.ui.screens.*
import com.example.canhbao.viewmodel.*

@Composable
fun NavGraph(authViewModel: AuthViewModel) {
    val webrtcViewModel: WebRTCViewModel = viewModel()
    val navController = rememberNavController()

    // 1. Khởi tạo API (Lấy từ class Retrofit bạn đã viết)

    // 2. Khởi tạo các ViewModel
    val mapViewModel: MapViewModel = viewModel()
    val sosViewModel: TinHieuSOSViewModel = viewModel()
    val lichSuViewModel: LichSuViewModel = viewModel()
    val apiService = com.example.canhbao.data.network.BaoCaoSuCoRetrofit.api

    // ✅ SỬA LỖI TẠI ĐÂY: Khởi tạo GoiViewModel bằng Factory
    val goiViewModel: GoiViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Đổi từ .instance thành .api ở đây
                return GoiViewModel(apiService) as T
            }
        }
    )

    val quaViewModel: QuaViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return QuaViewModel(apiService) as T
            }
        }
    )
    // 3. Theo dõi User
    val currentUser by authViewModel.user.collectAsState()

    // Xác định trang bắt đầu
   val startDest = if (currentUser != null) "map" else "login"
//    val startDest = "login"
    NavHost(navController = navController, startDestination = startDest) {

        composable("login") {
            LoginScreen(viewModel = authViewModel) {
                navController.navigate("map") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
        composable("map") {
            MapScreen(
                navController = navController,
                mapViewModel = mapViewModel,
                webrtcViewModel = webrtcViewModel, // <--- THÊM DÒNG NÀY VÀO ĐÂY
                isLoggedIn = currentUser != null,
                onReportClick = { navController.navigate("bao_cao_su_co") }
            )
        }

        composable("goi_screen") {
            GoiScreen(
                viewModel = goiViewModel,
                navController = navController
            )
        }
        composable("qua_screen") {
            QuaScreen(
                viewModel = quaViewModel,
                navController = navController
            )
        }
        // Trong NavGraph.kt
        composable("doitien_screen") {
            // Khởi tạo ViewModel riêng cho màn hình đổi tiền
            val doiTienVM: DoiTienViewModel = viewModel()
            DoiTienScreen(
                navController = navController,
                uid = currentUser?.uid,
                viewModel = doiTienVM // Truyền đúng DoiTienViewModel vào đây
            )
        }
        composable("home") {
            HomeScreen(
                userEmail = currentUser?.email,
                userPhotoUrl = currentUser?.photoUrl?.toString(),
                uid = currentUser?.uid,
                navController = navController,
                onLogout = {
                    authViewModel.logout()
                    // Xóa sạch stack và quay về login
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Các route khác giữ nguyên...
        composable(
            route = "tin_hieu_sos/{lat}/{lng}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            TinHieuSOSScreen(
                navController = navController,
                viewModel = sosViewModel,
                lat = lat,
                lng = lng,
                userId = currentUser?.uid ?: "GUEST"
            )
        }

        composable("tui_screen") {
            TuiScreen(
                viewModel = quaViewModel,
                uid = currentUser?.uid ?: "",
                onBack = { navController.popBackStack() } // Thêm dòng này để xử lý nút quay lại
            )
        }
        composable("lich_su") {
            LichSuScreen(
                navController = navController,
                viewModel = lichSuViewModel
            )
        }

        composable("bao_cao_su_co") {
            BaoCaoSuCoScreen(
                onBack = { navController.popBackStack() },
                navController = navController // Truyền thêm cái này
            )
        }

        composable("di_chuyen") {
            DiChuyenScreen(mapViewModel = mapViewModel, navController = navController)
        }
    }
}