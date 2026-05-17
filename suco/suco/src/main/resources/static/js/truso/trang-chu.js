mapboxgl.accessToken = MAPBOX_TOKEN;

window.map = new mapboxgl.Map({
  container: "map",
  style: "mapbox://styles/mapbox/streets-v12",
  center: [tsLng, tsLat],
  zoom: 15,
});

const activeMarkers = {};

map.on("load", () => {
  // 1. Vẽ vị trí trụ sở (Cái này phải hiện ngay lập tức)
  new mapboxgl.Marker({ color: "#10b981" })
    .setLngLat([tsLng, tsLat])
    .setPopup(new mapboxgl.Popup().setHTML("<b>Vị trí trụ sở</b>"))
    .addTo(map);

  // 2. Nạp dữ liệu song song (Parallel Loading)
  // Dùng Promise.all để trình duyệt gửi cả 2 yêu cầu cùng lúc
  Promise.all([loadExistingSOS(), loadExistingSuCo()]).then(() => {
    console.log("Tất cả icon đã nạp xong");
    connectWebSocket();
    handleRedirectParams();
  });
});

map.on("click", (e) => {
  if (e.originalEvent.target.classList.contains("mapboxgl-canvas")) {
    closeDetail();
  }
});

function closeDetail() {
  document.getElementById("sos-detail-panel").style.display = "none";
  removeRoute();
}

function removeRoute() {
  if (map.getSource("route")) {
    map.removeLayer("route");
    map.removeSource("route");
  }
}

async function drawRoute(targetLng, targetLat) {
  removeRoute();
  const query = await fetch(
    `https://api.mapbox.com/directions/v5/mapbox/driving/${tsLng},${tsLat};${targetLng},${targetLat}?geometries=geojson&access_token=${mapboxgl.accessToken}`,
  );
  const json = await query.json();
  if (!json.routes[0]) return;
  const data = json.routes[0].geometry;
  map.addSource("route", {
    type: "geojson",
    data: { type: "Feature", geometry: data },
  });
  map.addLayer({
    id: "route",
    type: "line",
    source: "route",
    layout: { "line-join": "round", "line-cap": "round" },
    paint: {
      "line-color": "#3b82f6",
      "line-width": 6,
      "line-opacity": 0.8,
    },
  });
}

function showSOSDetail(item) {
  const panel = document.getElementById("sos-detail-panel");
  const content = document.getElementById("panel-content");
  panel.style.display = "flex";

  const user = item.user || {};
  const userName = user.name || "Khách vãng lai";
  const userEmail = user.email || "Không có email";
  const userPoints = user.totalPoints || 0;
  const isPriority = userPoints >= 500;

  const id = item.id;
  const kinhDo = (item.kinhDo || 0).toFixed(6);
  const viDo = (item.viDo || 0).toFixed(6);
  const ghiChu = item.ghiChu || "Không có ghi chú";
  const thoiGian = item.createdAt
    ? new Date(item.createdAt).toLocaleString("vi-VN")
    : "Vừa xong";
  const trangThai = item.trangThai || "CHO_XU_LY";

  // SỬA TẠI ĐÂY: Logic fix URL chuẩn
  const fixUrl = (path) => {
    if (!path) return null;
    // Nếu path đã bắt đầu bằng /uploads thì trả về luôn, không nối thêm gì cả
    if (path.startsWith("/uploads") || path.startsWith("http")) {
      return path;
    }
    // Chỉ nối thêm folder nếu path chỉ là tên file đơn thuần
    return `/uploads/sos/${path}`;
  };

  const imgUrl = fixUrl(item.hinhAnh);
  const audioUrl = fixUrl(item.ghiAm);

  let actionButton = "";
  if (trangThai === "CHO_XU_LY") {
    actionButton = `<button class="btn-approve" onclick="doiTrangThai(${id}, 'DANG_XU_LY')">
                        <i class="fa-solid fa-truck-fast"></i> TIẾP NHẬN CỨU TRỢ</button>`;
  } else if (trangThai === "DANG_XU_LY") {
    actionButton = `<button class="btn-approve" style="background:#10b981" onclick="doiTrangThai(${id}, 'HOAN_THANH')">
                        <i class="fa-solid fa-check-double"></i> XÁC NHẬN HOÀN THÀNH</button>`;
  }

  content.innerHTML = `
        ${isPriority ? `<div style="background: linear-gradient(90deg, #FFD700, #FFA500); color: #000; padding: 5px 10px; border-radius: 5px; font-weight: bold; margin-bottom: 15px; text-align: center; font-size: 0.8rem;"><i class="fa-solid fa-gem"></i> NGƯỜI DÙNG ƯU TIÊN (KIM CƯƠNG)</div>` : ""}

        <div class="info-group">
            <div class="info-label">NGƯỜI GỬI TÍN HIỆU</div>
            <div class="info-value" style="font-weight: bold; font-size: 1.1rem; color: #1e293b;">${userName}</div>
            <div class="info-value" style="font-size: 0.85rem; color: #64748b;">${userEmail}</div>
        </div>

        <div class="info-group">
            <div class="info-label">Trạng thái & Thời gian</div>
            <div class="info-value">
                <span class="badge" style="background: #fee2e2; color: #ef4444; padding: 2px 8px; border-radius: 4px;">${trangThai}</span>
                <span style="margin-left: 10px; font-size: 0.85rem;"><i class="fa-regular fa-clock"></i> ${thoiGian}</span>
            </div>
        </div>

        <div class="info-group">
            <div class="info-label">Ghi chú hiện trường</div>
            <div class="info-value" style="background: #fff; border: 1px solid #e2e8f0; padding: 10px; border-radius: 6px; font-style: italic;">"${ghiChu}"</div>
        </div>

        <div class="info-group">
            <div class="info-label">Hình ảnh hiện trường</div>
            ${
              imgUrl
                ? `<img src="${imgUrl}" class="sos-image" style="width:100%; border-radius:8px; margin-top:5px; cursor:pointer;" onerror="this.src='https://placehold.co/400x300?text=Không+tìm+thấy+ảnh'" onclick="window.open(this.src)">`
                : '<div class="info-value">Không có ảnh</div>'
            }
        </div>

        <div class="info-group">
            <div class="info-label">Ghi âm khẩn cấp</div>
            ${
              audioUrl
                ? `<audio controls style="width: 100%; margin-top:5px;"><source src="${audioUrl}" type="audio/mpeg">Trình duyệt không hỗ trợ nghe audio.</audio>`
                : '<div class="info-value">Không có ghi âm</div>'
            }
        </div>

        <div style="margin-top: 20px; border-top: 1px solid #eee; padding-top: 15px;">
            ${actionButton}
        </div>
    `;

  map.flyTo({
    center: [parseFloat(kinhDo), parseFloat(viDo)],
    zoom: 17,
    speed: 1.2,
  });
  drawRoute(parseFloat(kinhDo), parseFloat(viDo));
}

function showSuCoDetail(item) {
  const panel = document.getElementById("sos-detail-panel");
  const content = document.getElementById("panel-content");
  panel.style.display = "flex";

  const tenLoai = item.tenLoai || "Sự cố";
  const moTa = item.moTa || "Không có mô tả";
  const icon = item.iconUrl || "https://placehold.co/30x30?text=";
  const mucDo = item.mucDoNghiemTrong || "NORMAL";

  // Lưu ý: Dùng trangThaiXuLy để điều khiển nút bấm
  const trangThai = item.trangThaiXuLy || "CHO_XU_LY";
  const imgUrl = item.hinhAnhUrl;

  const badgeColor =
    mucDo === "HIGH" ? "#ef4444" : mucDo === "MEDIUM" ? "#f59e0b" : "#10b981";

  // --- BƯỚC 1: XỬ LÝ LOGIC NÚT BẤM (CẬP NHẬT) ---
  let actionButton = "";
  if (trangThai === "CHO_XU_LY") {
    actionButton = `
            <button class="btn-approve" style="background:#1f2937" onclick="doiTrangThaiSuCo(${item.id}, 'DANG_XU_LY')">
                <i class="fa-solid fa-person-digging"></i> TIẾP NHẬN XỬ LÝ
            </button>`;
  } else if (trangThai === "DANG_XU_LY") {
    actionButton = `
            <div class="info-group" style="margin-top: 15px; border: 1px dashed #f59e0b; padding: 10px; border-radius: 8px;">
                <div class="info-label">CẬP NHẬT MỨC ĐỘ NGUY HIỂM</div>
                <select id="select-muc-do-${
                  item.id
                }" class="form-control" style="width: 100%; padding: 8px; border-radius: 5px; margin-bottom: 10px; border: 1px solid #ddd;">
                    <option value="LOW" ${
                      mucDo === "LOW" ? "selected" : ""
                    }>THẤP (LOW)</option>
                    <option value="MEDIUM" ${
                      mucDo === "MEDIUM" ? "selected" : ""
                    }>TRUNG BÌNH (MEDIUM)</option>
                    <option value="HIGH" ${
                      mucDo === "HIGH" ? "selected" : ""
                    }>CAO (HIGH)</option>
                </select>
                <button class="btn-approve" style="background:#f59e0b; margin-bottom: 8px; font-size: 0.8rem;" onclick="updateMucDo(${
                  item.id
                })">
                    <i class="fa-solid fa-pen-to-square"></i> CẬP NHẬT MỨC ĐỘ
                </button>
                <button class="btn-approve" style="background:#10b981" onclick="doiTrangThaiSuCo(${
                  item.id
                }, 'HOAN_THANH')">
                    <i class="fa-solid fa-check-double"></i> XÁC NHẬN HOÀN THÀNH
                </button>
            </div>`;
  } else {
    // Trạng thái HOAN_THANH hoặc bất kỳ trạng thái nào khác
    actionButton = `<div class="info-value" style="text-align:center; color: #10b981; font-weight: bold; padding: 10px; border: 1px solid #10b981; border-radius: 8px;">
                            <i class="fa-solid fa-circle-check"></i> SỰ CỐ ĐÃ HOÀN THÀNH
                        </div>`;
  }

  // --- BƯỚC 2: GÁN VÀO INNERHTML ---
  content.innerHTML = `
          <div class="info-group">
              <div class="info-label">LOẠI SỰ CỐ</div>
              <div class="info-value" style="font-weight: bold; display: flex; align-items: center; gap: 8px;">
                  <img src="${icon}" width="24" height="24">
                  <span>${tenLoai}</span>
              </div>
          </div>

          <div class="info-group">
              <div class="info-label">Mức độ & Trạng thái</div>
              <div class="info-value">
                  <span id="badge-muc-do" style="background: ${badgeColor}; color: white; padding: 2px 8px; border-radius: 4px; font-size: 11px;">${mucDo}</span>
                  <span style="background: #e5e7eb; padding: 2px 8px; border-radius: 4px; font-size: 11px; margin-left: 5px;">${trangThai}</span>
              </div>
          </div>

          <div style="display: flex; gap: 15px; margin-bottom: 16px;">
              <div style="flex: 1;">
                  <div class="info-label">Kinh độ</div>
                  <div class="info-value">${item.kinhDo}</div>
              </div>
              <div style="flex: 1;">
                  <div class="info-label">Vĩ độ</div>
                  <div class="info-value">${item.viDo}</div>
              </div>
          </div>

          <div class="info-group">
              <div class="info-label">Mô tả chi tiết</div>
              <div class="info-value" style="background: #fffbeb; padding: 12px; border-radius: 8px; border-left: 4px solid #f59e0b; font-style: italic;">
                  "${moTa}"
              </div>
          </div>

          <div class="info-group">
              <div class="info-label">Hình ảnh hiện trường</div>
              ${
                imgUrl
                  ? `<img src="${imgUrl}" class="sos-image" onerror="this.src='https://placehold.co/400x300?text=Lỗi+ảnh'" onclick="window.open(this.src)">`
                  : '<div class="info-value">Không có ảnh</div>'
              }
          </div>

          <div style="margin-top: 20px; border-top: 1px solid #eee; padding-top: 15px;">
              ${actionButton}
          </div>
      `;

  map.flyTo({
    center: [parseFloat(item.kinhDo), parseFloat(item.viDo)],
    zoom: 17,
    speed: 1.2,
  });
  drawRoute(parseFloat(item.kinhDo), parseFloat(item.viDo));
}
function updateMucDo(id) {
  const mucDoSelect = document.getElementById(`select-muc-do-${id}`);
  const mucDo = mucDoSelect.value;

  fetch(`/su-co/cap-nhat-muc-do/${id}?mucDo=${mucDo}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ mucDo }),
  }).then((res) => {
    if (res.ok) {
      alert("Đã cập nhật mức độ nghiêm trọng!");

      // Cập nhật lại dữ liệu trong bộ nhớ cục bộ
      const markerKey = "SU_CO_" + id;
      if (activeMarkers[markerKey]) {
        activeMarkers[markerKey].data.mucDoNghiemTrong = mucDo;

        // 1. Vẽ lại Marker trên bản đồ để đổi màu viền ngay lập tức
        addSOSMarker(activeMarkers[markerKey].data, "SU_CO");

        // 2. Vẽ lại nội dung Panel để cập nhật Badge màu
        showSuCoDetail(activeMarkers[markerKey].data);
      }
    } else {
      alert("Lỗi cập nhật!");
    }
  });
}
function getMarkerClass(status) {
  if (status === "DANG_XU_LY") return "marker-dang-xu-ly";
  if (status === "HOAN_THANH") return "marker-hoan-thanh";
  return "marker-cho-xu-ly";
}

function addSOSMarker(item, type = "SOS") {
  const markerKey = type + "_" + item.id;
  const trangThai = type === "SOS" ? item.trangThai : item.trangThaiXuLy;

  // --- 1. XÓA NẾU TRẠNG THÁI KHÔNG HỢP LỆ ---
  const forceDeleteStatus = ["HUY_BO", "DA_AN", "SPAM"];
  if (forceDeleteStatus.includes(trangThai)) {
    if (activeMarkers[markerKey]) {
      activeMarkers[markerKey].marker.remove();
      delete activeMarkers[markerKey];
      closeDetail();
    }
    return;
  }

  // --- 2. XÁC ĐỊNH MÀU SẮC THEO MỨC ĐỘ (Dành cho Sự cố) ---
  // --- 2. XÁC ĐỊNH MÀU SẮC THEO MỨC ĐỘ ---
  let themeColor = "#94a3b8"; // Mặc định luôn là màu XÁM

  if (type === "SU_CO") {
    // Chỉ đổi màu khi "Mức độ" KHÔNG PHẢI là null và KHÔNG PHẢI là "NONE" (hoặc giá trị mặc định chưa phân loại)
    // Giả sử khi chưa chọn mức độ, server trả về item.mucDoNghiemTrong là null hoặc ""
    if (item.mucDoNghiemTrong && item.mucDoNghiemTrong !== "NONE") {
      if (item.mucDoNghiemTrong === "HIGH") {
        themeColor = "#ef4444"; // Đỏ
      } else if (item.mucDoNghiemTrong === "MEDIUM") {
        themeColor = "#f59e0b"; // Vàng/Cam
      } else if (item.mucDoNghiemTrong === "LOW") {
        themeColor = "#10b981"; // Xanh lá
      }
    } else {
      // Nếu chưa có mức độ, giữ nguyên màu xám
      themeColor = "#94a3b8";
    }
  } else {
    // Logic màu cho SOS (giữ nguyên hoặc tùy chỉnh)
    if (trangThai === "CHO_XU_LY") themeColor = "#ff0000";
    else if (trangThai === "DANG_XU_LY") themeColor = "#f59e0b";
    else themeColor = "#10b981";
  }

  // --- 3. CẬP NHẬT NẾU ĐÃ TỒN TẠI (Đổi màu khung/hiệu ứng) ---
  if (activeMarkers[markerKey]) {
    activeMarkers[markerKey].data = item;
    const el = activeMarkers[markerKey].marker.getElement();
    const currentStatus = type === "SOS" ? item.trangThai : item.trangThaiXuLy;
    if (type === "SU_CO") {
      const pin = el.querySelector(".marker-pin-main");
      if (pin) {
        pin.style.borderColor = themeColor;
        // Nếu đã HOAN_THANH thì ẩn hoặc đổi hiệu ứng
        if (currentStatus === "HOAN_THANH") {
          pin.classList.remove("marker-pulse");
          // Tùy chọn: Ẩn marker sau 2 giây nếu muốn
          // setTimeout(() => activeMarkers[markerKey].marker.remove(), 2000);
        }
      }
    } else {
      const dot = el.querySelector(".simple-dot");
      if (dot) {
        dot.style.backgroundColor = themeColor;
        if (trangThai !== "CHO_XU_LY") dot.classList.remove("marker-pulse");
      }
    }
    return;
  }

  // --- 4. TẠO MỚI MARKER ---
  const el = document.createElement("div");
  el.className = "custom-marker-wrapper";

  if (type === "SU_CO") {
    // HIỂN THỊ KIỂU GHIM CÓ ICON CHO SỰ CỐ
    const isPulse = trangThai === "CHO_XU_LY" ? "marker-pulse" : "";
    const iconUrl =
      item.iconUrl || "https://cdn-icons-png.flaticon.com/512/564/564619.png";

    el.innerHTML = `
            <div class="marker-pin-main ${isPulse}" style="border-color: ${themeColor}">
                <img src="${iconUrl}" alt="icon">
            </div>
            <div class="marker-tail-fix" style="border-top-color: ${themeColor}"></div>
        `;
  } else {
    // HIỂN THỊ KIỂU CHẤM TRÒN CHO SOS
    const isPulse = trangThai === "CHO_XU_LY" ? "marker-pulse" : "";
    const isVip = item.isVip;
    const crownHtml = isVip
      ? `<div style="position: absolute; top: -20px; left: 50%; transform: translateX(-50%); color: #FFD700; font-size: 18px; text-shadow: 1px 1px 3px rgba(0,0,0,0.6); z-index: 10;">
                <i class="fa-solid fa-crown"></i>
             </div>`
      : "";

    el.innerHTML = `
            ${crownHtml}
            <div class="simple-dot ${isPulse}" 
                 style="background-color: ${themeColor}; width: 24px; height: 24px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(0,0,0,0.5); cursor: pointer;">
            </div>
        `;
  }

  const marker = new mapboxgl.Marker({
    element: el,
    anchor: type === "SU_CO" ? "bottom" : "center",
  })
    .setLngLat([parseFloat(item.kinhDo), parseFloat(item.viDo)])
    .addTo(map);

  activeMarkers[markerKey] = { marker, data: item, type };

  el.addEventListener("click", (e) => {
    e.stopPropagation();
    type === "SU_CO" ? showSuCoDetail(item) : showSOSDetail(item);
  });
}
function doiTrangThai(id, status) {
  const url = `/api/map/sos/cap-nhat-trang-thai/${id}?status=${status}&idTruSo=${idTruSo}`;
  fetch(url, { method: "POST" }).then((res) => {
    if (res.ok) {
      const markerKey = "SOS_" + id;
      if (activeMarkers[markerKey]) {
        // Cập nhật dữ liệu cục bộ
        activeMarkers[markerKey].data.trangThai = status;

        if (status === "HOAN_THANH") {
          // Xóa marker nếu hoàn thành
          activeMarkers[markerKey].marker.remove();
          delete activeMarkers[markerKey];
          closeDetail();
        } else {
          // Vẽ lại panel để nút đổi từ "Tiếp nhận" sang "Hoàn thành"
          showSOSDetail(activeMarkers[markerKey].data);
          addSOSMarker(activeMarkers[markerKey].data, "SOS");
        }
      }
    }
  });
}
function loadExistingSOS() {
  return fetch("/api/tin-hieu-sos/active/" + idTruSo)
    .then((res) => res.json())
    .then((data) => {
      // Sắp xếp dữ liệu theo thời gian mới nhất trước khi đổ vào chuông
      data.sort((a, b) => new Date(b.thoiGian) - new Date(a.thoiGian));

      data.forEach((item) => {
        addSOSMarker(item); // Hiện trên bản đồ
        // Thêm dòng dưới đây để hiện vào danh sách thông báo
        // Chú ý: truyền 'true' để không phát âm thanh dồn dập khi load trang
        addNotiItem(item, "SOS", true);
      });
      return data;
    });
}

function doiTrangThaiSuCo(id, status) {
  fetch(`/su-co/cap-nhat-trang-thai/${id}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      status: status,
    }),
  }).then((res) => {
    if (res.ok) {
      if (status === "HOAN_THANH") {
        const markerKey = "SU_CO_" + id;

        if (activeMarkers[markerKey]) {
          activeMarkers[markerKey].marker.remove();
          delete activeMarkers[markerKey];
        }

        closeDetail();
        alert("Đã hoàn thành và gỡ bỏ sự cố!");
      } else {
        const markerKey = "SU_CO_" + id;

        if (activeMarkers[markerKey]) {
          activeMarkers[markerKey].data.trangThaiXuLy = status; // Cập nhật trạng thái xử lý trong dữ liệu cục bộ
          addSOSMarker(activeMarkers[markerKey].data, "SU_CO"); // Cập nhật lại marker để đổi màu hoặc hiệu ứng
          showSuCoDetail(activeMarkers[markerKey].data); // Cập nhật lại panel nếu đang mở
        }

        alert("Đã tiếp nhận sự cố!");
      }
    }
  });
}
function loadExistingSuCo() {
  return fetch(`/api/map/su-co?idTruSo=${idTruSo}`)
    .then((res) => res.json())
    .then((data) => {
      data.forEach((item) => {
        addSOSMarker(item, "SU_CO");
        // Đưa sự cố vào chuông thông báo
        addNotiItem(item, "SU_CO", true);
      });
    });
}
function doiTrangThai(id, status) {
  // Truyền thêm &idTruSo=${idTruSo} vào URL (biến idTruSo lấy từ session đầu script)
  const url = `/api/tin-hieu-sos/cap-nhat-trang-thai/${id}?status=${status}&idTruSo=${idTruSo}`;

  fetch(url, { method: "POST" }).then((res) => {
    if (res.ok) {
      closeDetail();
      // WebSocket sẽ tự cập nhật lại Marker với dữ liệu mới có idTruSoTiepNhan
    } else {
      alert("Lỗi cập nhật!");
    }
  });
}

function handleRedirectParams() {
  const params = new URLSearchParams(window.location.search);
  const toLat = params.get("toLat");
  const toLng = params.get("toLng");
  const sosId = params.get("sosId");

  if (toLat && toLng) {
    drawRoute(parseFloat(toLng), parseFloat(toLat));
    map.flyTo({
      center: [parseFloat(toLng), parseFloat(toLat)],
      zoom: 17,
      speed: 1.2,
    });
  }

  if (sosId) {
    // If marker already exists, show detail; otherwise wait a bit for markers to be added
    const showIfReady = () => {
      const entry = activeMarkers[sosId];
      if (entry) {
        showSOSDetail(entry.data);
      } else {
        // try once more after small delay
        setTimeout(() => {
          const e2 = activeMarkers[sosId];
          if (e2) showSOSDetail(e2.data);
        }, 500);
      }
    };
    showIfReady();
  }
}
let bellCount = 0;

// 1. Hàm bật/tắt danh sách thông báo
function toggleNotiList() {
  const list = document.getElementById("noti-list");
  list.style.display = list.style.display === "none" ? "block" : "none";
}

// 2. Hàm thêm thông báo mới vào danh sách
// Thêm tham số isInitialLoad
function addNotiItem(data, type, isInitialLoad = false) {
  const container = document.getElementById("noti-items-container");
  const emptyMsg = document.getElementById("empty-noti");
  if (emptyMsg) emptyMsg.remove();

  bellCount++;
  const countEl = document.getElementById("noti-count");
  countEl.style.display = "flex";
  countEl.innerText = bellCount;

  // Chỉ rung chuông nếu KHÔNG PHẢI nạp dữ liệu cũ lúc load trang
  if (!isInitialLoad) {
    const bell = document.getElementById("noti-bell");
    bell.classList.add("animate-shake");
    setTimeout(() => bell.classList.remove("animate-shake"), 1000);
  }

  const time = data.thoiGian
    ? new Date(data.thoiGian).toLocaleTimeString("vi-VN")
    : "Vừa xong";
  const isVip = data.isVip || (data.user && data.user.totalPoints >= 500);

  const itemHtml = `
  <div class="noti-item" onclick="focusOnMarker('${type}_${data.id}')" 
       style="padding: 12px; border-bottom: 1px solid #f1f5f9; cursor: pointer; border-left: 4px solid ${type === "SOS" ? "#ef4444" : "#f59e0b"};">
      <div style="display: flex; gap: 10px; align-items: center;">
          <div style="flex: 1;">
              <div style="font-size: 13px; font-weight: 600;">${type === "SOS" ? "Tín hiệu SOS mới" : "Sự cố mới"}</div>
              <div style="font-size: 11px; color: #64748b;">${isVip ? "💎 Gói ưu tiên" : "Thông thường"} • ${time}</div>
          </div>
      </div>
  </div>`;

  container.insertAdjacentHTML("afterbegin", itemHtml);
}

// 3. Hàm xóa thông báo
function clearNoti(e) {
  e.stopPropagation();
  bellCount = 0;
  document.getElementById("noti-count").style.display = "none";
  document.getElementById("noti-items-container").innerHTML =
    '<div id="empty-noti" style="padding: 20px; text-align: center; color: #94a3b8; font-size: 13px;">Không có thông báo mới</div>';
}

// 4. Hàm hỗ trợ click vào thông báo để bay tới marker
function focusOnMarker(markerKey) {
  const entry = activeMarkers[markerKey];
  if (entry) {
    entry.type === "SU_CO"
      ? showSuCoDetail(entry.data)
      : showSOSDetail(entry.data);
  }
}

// 5. Cập nhật trong connectWebSocket (Ví dụ kênh SOS)
// Thay đoạn xử lý âm thanh trong kênh SOS bằng:
const playAlarm = () => {
  // Tiếng bíp dồn dập báo động từ thư viện chuẩn
  const audio = new Audio(
    "https://actions.google.com/sounds/v1/alarms/beep_short.ogg",
  );
  audio.volume = 1.0;

  // Phát âm thanh
  audio.play().catch((e) => {
    console.warn(
      "Trình duyệt chặn tự động phát. Hãy tương tác với bản đồ trước.",
    );
  });
};
function connectWebSocket() {
  const socket = new SockJS("/ws-suco");
  const stompClient = Stomp.over(socket);
  stompClient.debug = null;
  stompClient.connect({}, () => {
    // --- 1. KÊNH SOS ---
    stompClient.subscribe("/topic/truso/" + idTruSo, (msg) => {
      const sosData = JSON.parse(msg.body);
      const markerKey = "SOS_" + sosData.id;

      // Nếu là tín hiệu mới (chưa có trong activeMarkers)
      if (!activeMarkers[markerKey]) {
        playAlarm(); // Phát âm thanh
        addNotiItem(sosData, "SOS"); // Rung chuông + tăng số
      }

      // Logic ẩn/hiện dựa trên trạng thái
      if (sosData.trangThai === "HOAN_THANH") {
        if (activeMarkers[markerKey]) {
          activeMarkers[markerKey].marker.remove();
          delete activeMarkers[markerKey];
          closeDetail();
        }
      } else {
        addSOSMarker(sosData, "SOS"); // Thêm/Cập nhật marker
        if (activeMarkers[markerKey]) activeMarkers[markerKey].data = sosData;
      }

      // Cập nhật Panel nếu đang mở đúng cái này
      const panel = document.getElementById("sos-detail-panel");
      if (panel.style.display === "flex" && activeMarkers[markerKey]) {
        showSOSDetail(sosData);
      }
    });

    // --- 2. KÊNH ĐIỀU PHỐI ---
    stompClient.subscribe("/topic/truso/" + idTruSo + "/dieu-phoi", (msg) => {
      try {
        const thongBao = JSON.parse(msg.body);
        xuLyThongBaoDieuPhoiBanDo(thongBao);
      } catch (e) {
        console.error("Lỗi xử lý điều phối:", e);
      }
    });

    // --- 3. KÊNH XÓA SỰ CỐ (SPAM) ---
    stompClient.subscribe("/topic/su-co-delete", (msg) => {
      const reportId = JSON.parse(msg.body);
      const markerKey = "SU_CO_" + reportId;
      if (activeMarkers[markerKey]) {
        activeMarkers[markerKey].marker.remove();
        delete activeMarkers[markerKey];
        console.log("🚫 Đã xóa Marker Spam:", markerKey);
        closeDetail();
      }
    });

    // --- 4. KÊNH SỰ CỐ (Chỉnh lại để rung chuông) ---
    stompClient.subscribe("/topic/su-co", (msg) => {
      const updatedSuCo = JSON.parse(msg.body);
      const markerKey = "SU_CO_" + updatedSuCo.id;

      // Nếu là sự cố mới gửi tới bản đồ
      if (
        !activeMarkers[markerKey] &&
        updatedSuCo.trangThaiXuLy !== "HOAN_THANH"
      ) {
        playAlarm();
        addNotiItem(updatedSuCo, "SU_CO");
      }

      if (updatedSuCo.trangThaiXuLy === "HOAN_THANH") {
        if (activeMarkers[markerKey]) {
          activeMarkers[markerKey].marker.remove();
          delete activeMarkers[markerKey];
          closeDetail();
        }
      } else {
        addSOSMarker(updatedSuCo, "SU_CO");
        if (activeMarkers[markerKey])
          activeMarkers[markerKey].data = updatedSuCo;
      }
    });

    // --- 5. KÊNH SỰ CỐ RIÊNG CỦA TRỤ SỞ ---
    stompClient.subscribe("/topic/tru-so/" + idTruSo + "/su-co", (msg) => {
      const data = JSON.parse(msg.body);
      const markerKey = "SU_CO_" + data.id;

      // LOGIC XÓA KHI HOÀN THÀNH
      if (data.trangThaiXuLy === "HOAN_THANH") {
        if (activeMarkers[markerKey]) {
          activeMarkers[markerKey].marker.remove();
          delete activeMarkers[markerKey];
          closeDetail();
        }
        return;
      }

      addSOSMarker(data, "SU_CO");

      if (activeMarkers[markerKey]) {
        activeMarkers[markerKey].data = data;
      }

      const panel = document.getElementById("sos-detail-panel");
      if (panel.style.display === "flex" && activeMarkers[markerKey]) {
        showSuCoDetail(data);
      }
    });
  });
}
// Xử lý thông báo điều phối trên bản đồ
function xuLyThongBaoDieuPhoiBanDo(thongBao) {
  const markerKey = "SOS_" + thongBao.idSos; // Thêm tiền tố SOS_

  if (thongBao.loaiThongBao === "XOA_SOS") {
    if (activeMarkers[markerKey]) {
      activeMarkers[markerKey].marker.remove();
      delete activeMarkers[markerKey];
      console.log(`[BẢN ĐỒ] Đã xóa SOS #${thongBao.idSos}`);
      closeDetail();
    }
  }
}

function tiepNhanSOS(idTinHieu, maThietBi) {
  fetch(`/api/tin-hieu-sos/tiep-nhan/${idTinHieu}`, {
    method: "POST",
  }).then((res) => {
    if (res.ok) {
      alert("Đã tiếp nhận thành công!");
      if (activeMarkers[idTinHieu]) {
        activeMarkers[idTinHieu].remove();
        delete activeMarkers[idTinHieu];
      }
      closeDetail();
    }
  });
}
function logout() {
  fetch("/truso/logout", {
    method: "POST",
    credentials: "include",
  }).then(() => {
    window.location.href = "/truso/login";
  });
}
