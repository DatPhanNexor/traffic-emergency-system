package com.example.canhbao.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlaceAutocompleteTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: suspend (String) -> List<Pair<String, LatLng>>,
    onPlaceSelected: (LatLng, String) -> Unit,
    modifier: Modifier = Modifier,
    extraTrailingIcon: @Composable (() -> Unit)? = null // Giữ lại để dùng nút GPS bên DiChuyenScreen
) {
    var suggestions by remember { mutableStateOf<List<Pair<String, LatLng>>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Dùng Box để quản lý danh sách gợi ý đè lên bản đồ dễ hơn
    Box(modifier = modifier) {
        Column {
            TextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    scope.launch {
                        delay(400) // Tăng delay một chút để tránh gọi API quá liên tục
                        if (it.length > 2) suggestions = onSearch(it)
                        else suggestions = emptyList()
                    }
                },
                placeholder = { Text(label, color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                trailingIcon = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        if (value.isNotEmpty()) {
                            IconButton(onClick = {
                                onValueChange("")
                                suggestions = emptyList()
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Close, "Xóa", modifier = Modifier.size(20.dp))
                            }
                        }
                        // Hiển thị thêm nút GPS nếu có truyền vào (Dùng bên DiChuyenScreen)
                        extraTrailingIcon?.invoke()
                    }
                }
            )

            // DANH SÁCH GỢI Ý NỔI
            if (suggestions.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .heightIn(max = 250.dp)
                        .zIndex(100f), // Rất quan trọng để nổi lên trên Map
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    LazyColumn {
                        items(suggestions) { (name, latLng) ->
                            ListItem(
                                leadingContent = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp)) },
                                headlineContent = {
                                    Text(
                                        text = name,
                                        maxLines = 2, // Cho phép 2 dòng để đọc địa chỉ rõ hơn
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                },
                                modifier = Modifier.clickable {
                                    onPlaceSelected(latLng, name)
                                    suggestions = emptyList()
                                    focusManager.clearFocus()
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(0.5f))
                        }
                    }
                }
            }
        }
    }
}