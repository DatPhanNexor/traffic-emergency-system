// Các điều khiển trên bản đồ
map.addControl(new mapboxgl.NavigationControl(), "bottom-right");
const geocoder = new MapboxGeocoder({
  accessToken: mapboxgl.accessToken,
  mapboxgl: mapboxgl,
  placeholder: "Tìm địa chỉ...",
  countries: "vn",
  marker: false,
});
map.addControl(geocoder, "top-left");

// Marker đỏ để chọn vị trí
let pickerMarker = new mapboxgl.Marker({
  color: "#ff0000",
  draggable: true,
});

// --- TIỆN ÍCH BẢN ĐỒ ---
function saveMapState(lng, lat) {
  localStorage.setItem("map_lng", lng);
  localStorage.setItem("map_lat", lat);
}

function updateLocationFields(lng, lat) {
  const formattedLat = lat.toFixed(6);
  const formattedLng = lng.toFixed(6);
  document
    .querySelectorAll(".lat-field")
    .forEach((el) => (el.value = formattedLat));
  document
    .querySelectorAll(".lng-field")
    .forEach((el) => (el.value = formattedLng));
  saveMapState(lng, lat);
}

function clearAllMarkers() {
  currentMarkers.forEach((m) => m.remove());
  currentMarkers = [];
}

const tabs = document.querySelectorAll(".control-tab");
const panels = document.querySelectorAll(".side-panel");

tabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    const targetId = tab.getAttribute("data-target");
    const targetPanel = document.getElementById(targetId);
    if (tab.classList.contains("active")) {
      tab.classList.remove("active");
      targetPanel.classList.remove("open");
    } else {
      tabs.forEach((t) => t.classList.remove("active"));
      panels.forEach((p) => p.classList.remove("open"));
      tab.classList.add("active");
      targetPanel.classList.add("open");
    }
  });
});

// --- SỰ KIỆN BẢN ĐỒ ---
map.on("load", () => {
  if (savedLng && savedLat) {
    pickerMarker
      .setLngLat([parseFloat(savedLng), parseFloat(savedLat)])
      .addTo(map);
  }

  // Hiển thị tòa nhà 3D
  const layers = map.getStyle().layers;
  const labelLayerId = layers.find(
    (layer) => layer.type === "symbol" && layer.layout["text-field"],
  ).id;
  map.addLayer(
    {
      id: "3d-buildings",
      source: "composite",
      "source-layer": "building",
      filter: ["==", "extrude", "true"],
      type: "fill-extrusion",
      minzoom: 15,
      paint: {
        "fill-extrusion-color": "#e0e0e0",
        "fill-extrusion-height": ["get", "height"],
        "fill-extrusion-base": ["get", "min_height"],
        "fill-extrusion-opacity": 0.8,
      },
    },
    labelLayerId,
  );

  // Tải toàn bộ Marker ban đầu
  loadIncidentMarkers();
  loadTruSoMarkers();
  loadCameraMarkers();
});

map.on("click", (e) => {
  const { lng, lat } = e.lngLat;
  pickerMarker.setLngLat([lng, lat]).addTo(map);
  updateLocationFields(lng, lat);
});

geocoder.on("result", (e) => {
  const [lng, lat] = e.result.geometry.coordinates;
  pickerMarker.setLngLat([lng, lat]).addTo(map);
  updateLocationFields(lng, lat);
});

pickerMarker.on("dragend", () => {
  const lngLat = pickerMarker.getLngLat();
  updateLocationFields(lngLat.lng, lngLat.lat);
});

// --- HÀM TẢI DỮ LIỆU TỪ API ---
async function loadIncidentMarkers() {
  try {
    const response = await fetch("/admin/bao-cao-su-co/all-markers");
    const incidents = await response.json();

    // Xóa toàn bộ các marker đang hiện có trên bản đồ
    Object.values(incidentMarkersMap).forEach((m) => m.remove());
    incidentMarkersMap = {}; // Reset lại object quản lý

    incidents.forEach((suCo) => renderSingleIncident(suCo));
  } catch (error) {
    console.error("Lỗi tải marker sự cố:", error);
  }
}

async function loadTruSoMarkers() {
  try {
    const response = await fetch("/api/tru-so/all");
    const danhSachTruSo = await response.json();

    // 1. Xóa các marker cũ
    truSoMarkersList.forEach((m) => m.remove());
    truSoMarkersList = [];

    // 2. Vẽ các marker mới
    danhSachTruSo.forEach((ts) => {
      const lng = parseFloat(ts.kinhDo);
      const lat = parseFloat(ts.viDo);
      if (isNaN(lng) || isNaN(lat) || (lng === 0 && lat === 0)) return;

      const el = document.createElement("div");
      el.className = "tru-so-marker";
      el.innerHTML = `<i class="fa-solid fa-house-chimney-medical" style="color: #27ae60; font-size: 30px; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3));"></i>`;

      const marker = new mapboxgl.Marker({
        element: el,
        anchor: "bottom",
      })
        .setLngLat([lng, lat])
        .setPopup(new mapboxgl.Popup().setHTML(`<h4>${ts.tenTruSo}</h4>`))
        .addTo(map);

      truSoMarkersList.push(marker); // Lưu vào mảng quản lý
    });
  } catch (error) {
    console.error("Lỗi tải marker trụ sở:", error);
  }
}

async function loadCameraMarkers() {
  try {
    const response = await fetch("/admin/quan-ly-camera/all-json");
    const cameras = await response.json();

    // 1. Xóa toàn bộ marker camera cũ trên bản đồ
    cameraMarkersList.forEach((m) => m.remove());
    cameraMarkersList = [];

    // 2. Vẽ marker mới
    cameras.forEach((cam) => {
      const lng = parseFloat(cam.kinhDo);
      const lat = parseFloat(cam.viDo);
      if (isNaN(lng) || isNaN(lat) || (lng === 0 && lat === 0)) return;

      const el = document.createElement("div");
      el.className = "camera-marker";
      el.innerHTML = `<i class="fa-solid fa-camera-retro" style="color: #3b82f6; font-size: 24px;"></i>`;

      const marker = new mapboxgl.Marker({
        element: el,
        anchor: "center",
      })
        .setLngLat([lng, lat])
        .setPopup(new mapboxgl.Popup().setHTML(`<b>${cam.tenCamera}</b>`))
        .addTo(map);

      // Lưu vào danh sách để lần sau xóa
      cameraMarkersList.push(marker);
    });
  } catch (error) {
    console.error("Lỗi tải marker camera:", error);
  }
}

// --- HÀM XỬ LÝ GỬI FORM (SUBMIT) ---
async function handleAdminSubmit() {
  const form = document.getElementById("adminSubmitForm");
  const formData = new FormData(form);

  // Validation cơ bản
  if (!formData.get("viDo") || !formData.get("kinhDo")) {
    alert("Vui lòng chọn vị trí trên bản đồ bằng cách click!");
    return;
  }

  try {
    const response = await fetch("/admin/bao-cao-su-co/admin-submit", {
      method: "POST",
      body: formData, // FormData tự động xử lý Multipart (bao gồm file ảnh)
    });

    if (response.ok) {
      const savedReport = await response.json();
      alert("Đã tạo sự cố thành công và tự động gán cho trụ sở gần nhất!");

      form.reset();
      // Reset marker chọn vị trí
      pickerMarker.remove();
      // Tải lại toàn bộ marker để cập nhật UI
      loadIncidentMarkers();

      // Đóng panel form
      document
        .querySelector('.control-tab[data-target="form-su-co"]')
        .classList.remove("active");
      document.getElementById("form-su-co").classList.remove("open");
    } else {
      alert("Lỗi khi tạo báo cáo.");
    }
  } catch (error) {
    console.error("Error:", error);
    alert("Lỗi kết nối server.");
  }
}
async function handleSosSubmit() {
  const form = document.getElementById("sosSubmitForm");
  const select = document.getElementById("select-tru-so");
  const truSoId = select.value;
  const formData = new FormData(form);

  // 🔥 DEBUG LOG 1 - toàn bộ formData
  console.log("===== FORM DATA SOS SUBMIT =====");
  console.log([...formData.entries()]);

  console.log("SELECT VALUE:", select.value);

  let url = "/admin/quan-ly-tru-so/them";
  if (truSoId !== "") {
    console.log("===== GÁN TRỤ SỞ CŨ =====");
    console.log("truSoId:", truSoId);
    console.log("kinhDo:", formData.get("kinhDo"));
    console.log("viDo:", formData.get("viDo"));

    const kinhDo = formData.get("kinhDo");
    const viDo = formData.get("viDo");
    // Bạn cần đảm bảo trong AdminTruSoController có API tương tự gan-toa-do
    url = `/admin/quan-ly-tru-so/gan-toa-do/${truSoId}?kinhDo=${kinhDo}&viDo=${viDo}`;

    const response = await fetch(url, { method: "POST" });
    if (response.ok) {
      alert("✅ Đã gán vị trí cho trụ sở thành công!");
      loadTruSoMarkers();
      pickerMarker.remove();
      form.reset();
    }
    return;
  }
  // Kiểm tra dữ liệu hợp lệ
  // if (
  //   select.value === "" &&
  //   !form.querySelector('input[name="tenTruSo"]').value
  // ) {
  //   alert("Vui lòng nhập tên trụ sở mới!");
  //   return;
  // }

  try {
    console.log("===== TẠO TRỤ SỞ MỚI =====");
    console.log("tenTruSo:", formData.get("tenTruSo"));
    console.log("tenDangNhap:", formData.get("tenDangNhap"));
    console.log("matKhau:", formData.get("matKhau"));
    console.log("kinhDo:", formData.get("kinhDo"));
    console.log("viDo:", formData.get("viDo"));
    const response = await fetch("/admin/quan-ly-tru-so/them", {
      method: "POST",
      body: formData,
    });

    if (response.ok) {
      alert("✅ Cập nhật trụ sở thành công!");

      // QUAN TRỌNG: Gọi hàm này để vẽ icon mới lên bản đồ ngay lập tức
      loadTruSoMarkers();

      // Reset form và ẩn các trường nhập liệu
      form.reset();
      toggleNewStationFields();

      // Xóa marker đỏ (picker) sau khi gán xong (tùy chọn)
      if (pickerMarker) pickerMarker.remove();
    } else {
      alert("❌ Lỗi: " + (await response.text()));
    }
  } catch (error) {
    console.error("Lỗi kết nối SOS:", error);
    alert("❌ Không thể kết nối đến máy chủ");
  }
}
function playNotificationSound() {
  // Sử dụng âm thanh Siren hoặc Emergency Alert
  const audio = new Audio(
    "https://assets.mixkit.co/active_storage/sfx/951/951-preview.mp3", // Tiếng còi báo động nhanh
  );
  audio.volume = 0.7; // Chỉnh âm lượng vừa phải
  audio.play().catch((e) => {
    console.log("Cần tương tác với trang web để phát thanh báo động");
  });
}
function toggleFormMode() {
  const select = document.getElementById("select-camera");
  const fieldsGroup = document.getElementById("camera-fields-group");
  const tenInput = document.getElementById("inputTenCamera");

  if (select.value !== "") {
    // CHẾ ĐỘ GÁN: Nếu chọn camera cũ (value khác rỗng)
    fieldsGroup.style.display = "none"; // Ẩn ngay lập tức các trường thông tin
    tenInput.required = false; // Bỏ bắt buộc nhập
  } else {
    // CHẾ ĐỘ TẠO MỚI: Nếu chọn "-- TẠO CAMERA MỚI --"
    fieldsGroup.style.display = "block"; // Hiện lại các trường thông tin
    tenInput.required = true; // Bắt buộc nhập tên
  }
}

async function handleCameraSubmit() {
  const form = document.getElementById("cameraSubmitForm");
  const select = document.getElementById("select-camera");
  const formData = new FormData(form);

  const lat = formData.get("viDo");
  const lng = formData.get("kinhDo");

  if (!lat || !lng) {
    alert("Vui lòng click chọn vị trí trên bản đồ trước!");
    return;
  }

  let url = "";
  let options = {};

  // CHẾ ĐỘ 1: THÊM MỚI HOÀN TOÀN
  if (select.value === "") {
    if (!formData.get("tenCamera")) {
      alert("Vui lòng nhập tên Camera mới!");
      return;
    }
    url = "/admin/quan-ly-camera/them";
    options = {
      method: "POST",
      body: formData,
    };
  }
  // CHẾ ĐỘ 2: GÁN TỌA ĐỘ CHO CAMERA CŨ
  else {
    // Sử dụng URLSearchParams để đảm bảo tham số được mã hóa đúng format
    const params = new URLSearchParams({
      kinhDo: lng,
      viDo: lat,
    });
    url = `/admin/quan-ly-camera/gan-toa-do/${select.value}?${params.toString()}`;
    options = {
      method: "POST",
    };
  }

  try {
    const response = await fetch(url, options);

    // Kiểm tra nếu response không ok thì văng lỗi ngay để rơi vào catch
    if (!response.ok) {
      const errorText = await response.text();
      throw new errorText();
    }

    const result = await response.text();
    alert("✅ Thành công: " + result);

    // RESET UI SAU KHI THÀNH CÔNG
    if (pickerMarker) pickerMarker.remove();

    // Gọi hàm tải lại marker một cách an toàn
    await loadCameraMarkers();

    // Đóng panel và reset form
    const cameraPanel = document.getElementById("form-camera");
    const cameraTab = document.querySelector(
      '.control-tab[data-target="form-camera"]',
    );
    if (cameraPanel) cameraPanel.classList.remove("open");
    if (cameraTab) cameraTab.classList.remove("active");

    form.reset();
    toggleFormMode();
  } catch (err) {
    console.error("Chi tiết lỗi:", err);
    alert("❌ Có lỗi xảy ra: " + err);
  }
}

// --- LOGIC ẨN/HIỆN TRƯỜNG NHẬP LIỆU MỚI ---
function toggleNewStationFields() {
  const select = document.getElementById("select-tru-so");
  const fields = document.getElementById("new-station-fields");
  if (!select || !fields) return;
  const inputs = fields.querySelectorAll("input");
  if (select.value !== "") {
    fields.style.display = "none";
    inputs.forEach((i) => i.removeAttribute("required"));
  } else {
    fields.style.display = "block";
    inputs.forEach((i) => i.setAttribute("required", "required"));
  }
}

function toggleNewCameraFields() {
  const select = document.getElementById("select-camera");
  const fields = document.getElementById("new-camera-fields");
  if (!select || !fields) return;
  const inputs = fields.querySelectorAll("input");
  if (select.value !== "") {
    fields.style.display = "none";
    inputs.forEach((i) => i.removeAttribute("required"));
  } else {
    fields.style.display = "block";
    const nameInput = fields.querySelector('input[name="tenCamera"]');
    if (nameInput) nameInput.setAttribute("required", "required");
  }
}

// --- WEBSOCKET & THÔNG BÁO ---
function connectWebSocket() {
  const socket = new SockJS("/ws-suco");
  const stompClient = Stomp.over(socket);
  stompClient.connect(
    {},
    function (frame) {
      // Kênh 1: Chỉ hiện tin nhắn text ở chuông báo
      stompClient.subscribe("/topic/admin-notifications", function (message) {
        updateNotificationList(message.body);
      });

      // Kênh 2: Nhận nguyên Object sự cố để VẼ ICON NGAY LẬP TỨC
      // Kênh 2: Nhận nguyên Object sự cố để VẼ/CẬP NHẬT ICON NGAY LẬP TỨC
      stompClient.subscribe("/topic/su-co", function (message) {
        const suCoDto = JSON.parse(message.body);
        console.log("Admin nhận dữ liệu cập nhật realtime:", suCoDto);

        // TRƯỜNG HỢP 1: Bị hủy hoặc Từ chối -> Xóa marker
        if (
          suCoDto.trangThaiXuLy === "HUY_BO" ||
          suCoDto.trangThaiDuyet === "REJECTED"
        ) {
          if (
            typeof incidentMarkersMap !== "undefined" &&
            incidentMarkersMap[suCoDto.id]
          ) {
            incidentMarkersMap[suCoDto.id].remove();
            delete incidentMarkersMap[suCoDto.id];
            loadPendingReports(); // Cập nhật sidebar
          }
        }
        // TRƯỜNG HỢP 2: Cập nhật Mức độ, Trạng thái, hoặc Hoàn thành
        else {
          // Hàm renderSingleIncident của bạn đã có logic:
          // - Nếu HOAN_THANH thì tự remove marker.
          // - Nếu ID đã tồn tại thì tự xóa cũ vẽ mới (để cập nhật màu sắc/icon).
          renderSingleIncident(suCoDto);

          // Phát âm thanh nếu là sự cố mới được đẩy lên (AI_APPROVED)
          if (
            suCoDto.trangThaiDuyet === "AI_APPROVED" &&
            suCoDto.trangThaiXuLy === "CHO_XU_LY"
          ) {
            playNotificationSound();
          }

          // Luôn làm mới danh sách chờ duyệt ở sidebar để Admin nắm bắt dữ liệu
          loadPendingReports();
        }
      });
    },
    function (err) {
      console.log("WebSocket error, reconnecting...");
      setTimeout(connectWebSocket, 5000);
    },
  );
}

// Hàm tải danh sách sự cố cần duyệt
async function loadPendingReports() {
  try {
    const response = await fetch("/admin/bao-cao-su-co/all-markers");
    const reports = await response.json();

    // Lọc chỉ lấy những bản ghi có trạng thái PENDING hoặc chưa duyệt
    const pendingReports = reports.filter(
      (r) =>
        r.trangThaiDuyet === "PENDING" || r.trangThaiDuyet === "AI_APPROVED",
    );

    const list = document.getElementById("notification-list");
    const badge = document.querySelector("#notification-bell .badge");

    // Xóa nội dung cũ
    list.innerHTML = "";

    if (pendingReports.length === 0) {
      list.innerHTML =
        '<div class="no-notification">Không có báo cáo cần duyệt</div>';
      badge.innerText = 0;
      badge.style.display = "none";
      return;
    }

    badge.innerText = pendingReports.length;
    badge.style.display = "block";

    // Sắp xếp theo id từ cao nhất đến thấp nhất (mới nhất lên trên)
    pendingReports.sort((a, b) => b.id - a.id);

    pendingReports.forEach((report) => {
      const item = document.createElement("div");
      item.className = "notification-item";

      const loaiSuCo = report.loaiSuCo ? report.loaiSuCo.ten : "Sự cố";
      const moTa = report.moTa || "Không có mô tả";
      const formattedDate = new Date(report.thoiGianTao).toLocaleString(
        "vi-VN",
      );

      // Hiển thị tương tự trang báo cáo: mô tả, loại, vị trí, độ tin cậy
      item.innerHTML = `
              <div style="display: flex; gap: 10px; align-items: center; justify-content: space-between; padding: 0;">
                <div style="flex: 1; min-width: 0;">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <strong style="color: #1e293b; font-size: 13px;">${loaiSuCo}</strong>
                    <span style="font-size: 10px; color: #3b82f6; font-weight: 600;">${report.trangThaiDuyet === "PENDING" ? "CHỜ DUYỆT" : "AI XÁC NHẬN"}</span>
                  </div>
                </div>
                <button onclick="goToReview(${report.id})" style="padding: 6px 10px; background: #3b82f6; color: white; border: none; border-radius: 6px; cursor: pointer; font-size: 11px; white-space: nowrap; flex-shrink: 0;">
                  Duyệt
                </button>
              </div>
              <div class="time">${formattedDate}</div>
            `;

      list.appendChild(item);
    });
  } catch (error) {
    console.error("Lỗi tải danh sách sự cố:", error);
  }
}

// Hàm chuyển đến trang duyệt
function goToReview(reportId) {
  window.location.href = `/admin/bao-cao-su-co?id=${reportId}`;
}

function renderSingleIncident(suCo) {
  const id = suCo.id;

  // 1. KIỂM TRA TRẠNG THÁI: Nếu đã hoàn thành thì XÓA Marker khỏi bản đồ
  if (suCo.trangThaiXuLy === "HOAN_THANH" || suCo.trangThaiXuLy === "HUY_BO") {
    if (incidentMarkersMap[id]) {
      incidentMarkersMap[id].remove(); // Xóa icon trên Mapbox
      delete incidentMarkersMap[id]; // Xóa khỏi danh sách quản lý
      console.log(
        "Realtime: Đã xóa/ẩn sự cố ID " +
          id +
          " do trạng thái: " +
          suCo.trangThaiXuLy,
      );
    }
    return; // Kết thúc hàm
  }

  // 2. CẬP NHẬT: Nếu Marker đã tồn tại nhưng chưa ẩn, xóa cái cũ để vẽ lại cái mới
  // (Phòng trường hợp đổi màu sắc hoặc icon khi dữ liệu thay đổi)
  if (incidentMarkersMap[id]) {
    incidentMarkersMap[id].remove();
  }

  // --- Giữ nguyên logic tính toán tọa độ và giao diện của bạn ---
  const lng = parseFloat(suCo.kinhDo);
  const lat = parseFloat(suCo.viDo);
  if (isNaN(lng) || isNaN(lat)) return;

  const isWaitingAdmin =
    suCo.trangThaiDuyet === "AI_APPROVED" || suCo.trangThaiDuyet === "PENDING";
  let borderColor = "#ffffff";
  if (suCo.trangThaiDuyet === "VERIFIED") {
    if (suCo.mucDoNghiemTrong === "HIGH") borderColor = "#e74c3c";
    else if (suCo.mucDoNghiemTrong === "MEDIUM") borderColor = "#f1c40f";
    else if (suCo.mucDoNghiemTrong === "LOW") borderColor = "#2ecc71";
  }

  const tenHienThi =
    suCo.tenLoai || (suCo.loaiSuCo ? suCo.loaiSuCo.ten : "Sự cố");
  const iconPath = suCo.iconUrl || (suCo.loaiSuCo ? suCo.loaiSuCo.iconUrl : "");

  const el = document.createElement("div");
  el.className = "custom-marker" + (isWaitingAdmin ? " pending-marker" : "");
  let iconHTML = isWaitingAdmin
    ? `<i class="fa-solid fa-circle-exclamation" style="color: #3498db; font-size: 24px;"></i>`
    : `<div class="marker-icon" style="background-image: url('${iconPath}'); width: 30px; height: 30px;"></div>`;

  el.innerHTML = `
        <div class="marker-pin" style="border-color: ${borderColor}; background-color: white;">
            ${iconHTML}
        </div>
        <div class="marker-tail" style="background-color: ${borderColor};"></div>`;

  const popup = new mapboxgl.Popup({ offset: 35 }).setHTML(`
        <div style="width:200px">
            <img src="${suCo.hinhAnhUrl || "/images/default-incident.jpg"}"
                 style="width:100%; border-radius:8px; height:100px; object-fit: cover;"
                 onerror="this.src='/images/default-incident.jpg'"/>
            <h4 style="margin:5px 0;">${tenHienThi}</h4>
            <p style="font-size:12px; margin:0; color:#666;">${
              suCo.moTa || "Không có mô tả"
            }</p>
            <button onclick="window.location.href='/admin/quan-ly-su-co?id=${
              suCo.id
            }'"
                    style="width:100%; margin-top:10px; padding:8px; cursor:pointer; background:#1a1a1a; color:white; border:none; border-radius:4px;">
                XỬ LÝ NGAY
            </button>
        </div>`);

  // 3. TẠO MARKER MỚI VÀ LƯU VÀO MAP THEO ID
  const marker = new mapboxgl.Marker({ element: el, anchor: "bottom" })
    .setLngLat([lng, lat])
    .setPopup(popup)
    .addTo(map);

  // Lưu lại để quản lý theo ID
  incidentMarkersMap[id] = marker;
}

function updateNotificationList(message) {
  notificationCount++;
  const bell = document.getElementById("notification-bell");
  const badge = bell.querySelector(".badge");
  const list = document.getElementById("notification-list");

  badge.innerText = notificationCount;
  badge.style.display = "block";
  bell.classList.add("ring-animation");

  const noNoti = list.querySelector(".no-notification");
  if (noNoti) noNoti.remove();

  const newItem = document.createElement("div");
  newItem.className = "notification-item";
  newItem.innerHTML = `
            <div style="display: flex; gap: 10px; align-items: flex-start;">
                <i class="fa-solid fa-circle-exclamation" style="color: #ef4444; margin-top: 4px;"></i>
                <div>
                    <div style="font-weight: 600; font-size: 13px;">Sự cố mới cần duyệt</div>
                    <div style="font-size: 12px; color: #555;">${message}</div>
                    <div class="time">${new Date().toLocaleTimeString(
                      "vi-VN",
                    )}</div>
                </div>
            </div>`;

  // clearAllMarkers();
  //loadIncidentMarkers();
  list.insertBefore(newItem, list.firstChild);
}

// --- KHỞI CHẠY KHI TẢI TRANG ---
document.addEventListener("DOMContentLoaded", () => {
  connectWebSocket();
  toggleNewStationFields();
  toggleNewCameraFields();
  loadPendingReports(); // Tải danh sách sự cố cần duyệt lần đầu

  const bellIcon = document.getElementById("notification-bell");
  const dropdown = document.getElementById("notification-dropdown");

  bellIcon.addEventListener("click", (e) => {
    e.stopPropagation();
    // Tải lại danh sách khi click vào chuông
    if (!dropdown.classList.contains("show")) {
      loadPendingReports();
    }
    dropdown.classList.toggle("show");
    if (dropdown.classList.contains("show")) {
      bellIcon.classList.remove("ring-animation");
    }
  });

  document.addEventListener("click", (e) => {
    if (!dropdown.contains(e.target) && !bellIcon.contains(e.target)) {
      dropdown.classList.remove("show");
    }
  });
});
