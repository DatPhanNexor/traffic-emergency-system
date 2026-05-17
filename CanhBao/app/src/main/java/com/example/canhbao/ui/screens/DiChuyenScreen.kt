package com.example.canhbao.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.canhbao.ui.components.PlaceAutocompleteTextField
import com.example.canhbao.viewmodel.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.Point
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiChuyenScreen(
    mapViewModel: MapViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    var activeField by remember { mutableIntStateOf(1) }
    var selectedVehicle by remember { mutableStateOf("Xe máy") }

    val primaryBlue = Color(0xFF1976D2)
    val backgroundGray = Color(0xFFF7F9FC)

    val recentHistory = listOf(
        "Trường Đại học Bách Khoa",
        "Quận 1, TP.HCM",
        "Sân bay Tân Sơn Nhất"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lên kế hoạch đi", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = backgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Card nhập liệu tinh tế hơn
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cột vẽ đường kẻ nối 2 điểm
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(24.dp)
                    ) {
                        Icon(Icons.Default.RadioButtonChecked, null, tint = primaryBlue, modifier = Modifier.size(16.dp))
                        Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray))
                        Icon(Icons.Default.LocationOn, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        PlaceAutocompleteTextField(
                            label = "Điểm đi",
                            value = startText,
                            onValueChange = { startText = it },
                            onSearch = { query ->
                                mapViewModel.searchPlaces(query).map { (name, point) ->
                                    name to LatLng(point.latitude(), point.longitude())
                                }
                            },
                            onPlaceSelected = { latLng, name ->
                                startText = name
                                mapViewModel.setStartPoint(Point.fromLngLat(latLng.longitude, latLng.latitude), name)
                            },
                            modifier = Modifier.onFocusChanged { if (it.isFocused) activeField = 1 },
                            extraTrailingIcon = {
                                IconButton(onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        scope.launch {
                                            val loc = fusedLocationClient.lastLocation.await()
                                            loc?.let {
                                                val point = Point.fromLngLat(it.longitude, it.latitude)
                                                startText = "Vị trí của tôi"
                                                mapViewModel.setStartPoint(point, "Vị trí của tôi")
                                            }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.MyLocation, null, tint = primaryBlue)
                                }
                            }
                        )

                        Spacer(Modifier.height(8.dp))

                        PlaceAutocompleteTextField(
                            label = "Điểm đến",
                            value = endText,
                            onValueChange = { endText = it },
                            onSearch = { query ->
                                mapViewModel.searchPlaces(query).map { (name, point) ->
                                    name to LatLng(point.latitude(), point.longitude())
                                }
                            },
                            onPlaceSelected = { latLng, name ->
                                endText = name
                                mapViewModel.setEndPoint(Point.fromLngLat(latLng.longitude, latLng.latitude), name)
                            },
                            modifier = Modifier.onFocusChanged { if (it.isFocused) activeField = 2 }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Phương tiện di chuyển", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VehicleItem("Xe máy", Icons.Default.DirectionsBike, selectedVehicle == "Xe máy", primaryBlue) { selectedVehicle = "Xe máy" }
                VehicleItem("Ô tô", Icons.Default.DirectionsCar, selectedVehicle == "Ô tô", primaryBlue) { selectedVehicle = "Ô tô" }
                VehicleItem("Đi bộ", Icons.Default.DirectionsWalk, selectedVehicle == "Đi bộ", primaryBlue) { selectedVehicle = "Đi bộ" }
            }

            Spacer(Modifier.height(16.dp))

            // Phần Lịch sử Gần đây
            Text("Địa điểm gần đây", style = MaterialTheme.typography.titleSmall, color = Color.Gray)

            Card(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                LazyColumn {
                    items(recentHistory) { loc ->
                        ListItem(
                            leadingContent = {
                                Icon(Icons.Default.History, null, tint = Color.LightGray, modifier = Modifier.background(backgroundGray, CircleShape).padding(8.dp))
                            },
                            headlineContent = { Text(loc, fontSize = 15.sp) },
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val res = mapViewModel.searchPlaces(loc)
                                    if (res.isNotEmpty()) {
                                        val (name, point) = res[0]
                                        if (activeField == 1) {
                                            startText = name
                                            mapViewModel.setStartPoint(point, name)
                                        } else {
                                            endText = name
                                            mapViewModel.setEndPoint(point, name)
                                        }
                                    }
                                }
                            }
                        )
                        if (loc != recentHistory.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = backgroundGray)
                    }
                }
            }

            // Nút Bắt đầu - làm to và nổi bật
            Button(
                onClick = {
                    scope.launch {
                        mapViewModel.setTravelMode(selectedVehicle)
                        mapViewModel.getFreeDirections()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = startText.isNotEmpty() && endText.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(12.dp))
                Text("BẮT ĐẦU CHỈ ĐƯỜNG", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun VehicleItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Icon(
            icon,
            null,
            tint = if (isSelected) activeColor else Color.Gray,
            modifier = Modifier.size(28.dp)
        )
        Text(
            name,
            color = if (isSelected) activeColor else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}