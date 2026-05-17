var CURRENT_VIEW = "sos";
function fixUrl(path) {
  if (!path) return null;
  if (path.startsWith("http") || path.startsWith("/uploads/")) {
    return path;
  }
  return `/uploads/sos/${path}`;
}
function formatTime(iso) {
  try {
    const d = new Date(iso);
    return d.toLocaleString();
  } catch (e) {
    return iso;
  }
}

// IDs of SOS this client just accepted (ignore realtime echo)
const handledSOS = new Set();

function renderSOSItem(sos) {
  const time = sos.createdAt ? formatTime(sos.createdAt) : sos.thoiGian || "";
  const userPoints =
    sos.user && sos.user.totalPoints ? sos.user.totalPoints : 0;
  const userName = sos.user && sos.user.name ? sos.user.name : "Khách vãng lai";

  // Logic địa chỉ: Nếu có diaChi thì dùng, không thì dùng tọa độ
  const displayAddress = sos.diaChi || `${sos.viDo}, ${sos.kinhDo}`;

  return `
          <div class="col-12 mb-2" id="sos-card-${sos.id}"
               data-lat="${sos.viDo || ""}"
               data-lng="${sos.kinhDo || ""}"
               data-points="${userPoints}"
               data-json='${encodeURIComponent(JSON.stringify(sos))}'>
            <div class="card card-sos ${userPoints > 100 ? "border-primary" : ""}">
              <div class="d-flex align-items-center w-100">
                <span class="badge bg-warning text-dark me-2" style="min-width: 70px;">
                  <i class="fa-solid fa-star"></i> ${userPoints} pts
                </span>

                <!-- Đồng hồ đếm ngược 60s -->
                <span class="countdown-timer me-2" id="timer-sos-${sos.id}">
                  <i class="fa-solid fa-clock"></i> 60s
                </span>

                <div class="flex-grow-1 d-flex flex-column">
                    <div class="d-flex align-items-center" style="gap:12px;">
                      <span><strong>Người gửi:</strong> ${userName}</span>
                      <span class="text-truncate" style="max-width: 350px;" title="${displayAddress}">
                          <i class="fa-solid fa-location-dot text-danger"></i>
                          <strong>Địa chỉ:</strong> ${displayAddress}
                      </span>
                      <i class="fa-solid fa-circle-info info-icon text-primary" title="Xem chi tiết" onclick="openSOSDetail(${sos.id})"></i>
                    </div>
                    <div class="text-muted small">
                      <strong>Ghi chú:</strong> ${sos.ghiChu || "Không có"}
                    </div>
                </div>

                <small class="text-muted ms-auto me-3">${time}</small>

                <div class="btn-group-action d-flex">
                  <button id="btn-accept-${sos.id}" onclick="confirmRescue(${sos.id})" class="btn btn-primary btn-sm shadow-sm">
                    <i class="fa-solid fa-check"></i> Tiếp nhận
                  </button>
                  <button id="btn-decline-${sos.id}" onclick="tuChoiTiepNhan(${sos.id})" class="btn btn-outline-secondary btn-sm">
                    <i class="fa-solid fa-xmark"></i> Không tiếp nhận
                  </button>
                </div>
              </div>
            </div>
          </div>`;
}

// Lưu trữ interval đếm ngược cho từng SOS (để có thể clear khi cần)
var demNguocIntervals = {};

function appendSOS(sos) {
  const list = document.getElementById("sos-list");
  const noDataElem = document.getElementById("no-data");
  if (noDataElem) noDataElem.style.display = "none";
  // prepend so newest is first
  // If this SOS was just handled by this client, don't duplicate
  if (handledSOS.has(sos.id)) return;
  list.innerHTML = renderSOSItem(sos) + list.innerHTML;

  // Bắt đầu đếm ngược cho SOS mới
  batDauDemNguocInline(sos.id, 60);
}

function loadPendingSOS() {
  if (!TRUSO_ID || TRUSO_ID === 0) return;

  fetch("/api/tin-hieu-sos/active/" + TRUSO_ID)
    .then((res) => res.json())
    .then((data) => {
      const list = document.getElementById("sos-list");
      if (!Array.isArray(data) || data.length === 0) {
        // ... xử lý no-data như cũ ...
        return;
      }

      // --- LOGIC SẮP XẾP MỚI ---
      data.sort((a, b) => {
        const pointsA = a.user && a.user.totalPoints ? a.user.totalPoints : 0;
        const pointsB = b.user && b.user.totalPoints ? b.user.totalPoints : 0;

        // 1. Ưu tiên điểm số giảm dần
        if (pointsB !== pointsA) {
          return pointsB - pointsA;
        }
        // 2. Nếu bằng điểm nhau, ai gửi trước (hoặc sau) thì tùy bạn (ở đây là mới nhất lên đầu)
        return new Date(b.createdAt) - new Date(a.createdAt);
      });

      // Lọc các trạng thái chờ xử lý
      const pendingSOS = data.filter((item) => {
        const status = item.trangThai || item.status;
        return status === "CHO_XU_LY";
      });

      const noDataElem = document.getElementById("no-data");
      if (pendingSOS.length === 0) {
        if (noDataElem) noDataElem.style.display = "block";
        list.innerHTML = "";
        return;
      }

      if (noDataElem) noDataElem.style.display = "none";
      list.innerHTML = ""; // Xóa danh sách cũ
      pendingSOS.forEach((sos) => {
        list.innerHTML += renderSOSItem(sos);
      });

      // Khởi động đếm ngược cho tất cả SOS đã load
      // Tính thời gian còn lại dựa trên createdAt
      pendingSOS.forEach((sos) => {
        let thoiGianConLai = 60;
        if (sos.createdAt) {
          const createdTime = new Date(sos.createdAt).getTime();
          const now = Date.now();
          const giayDaQua = Math.floor((now - createdTime) / 1000);
          thoiGianConLai = Math.max(5, 60 - giayDaQua); // Ít nhất 5s để xem
        }
        batDauDemNguocInline(sos.id, thoiGianConLai);
      });
    });
}

// --- SỰ CỐ VIEW ---
function renderSuCoItem(suCo) {
  const time = suCo.createdAt || suCo.thoiGian || "";
  const displayAddress = suCo.diaChi || `${suCo.viDo}, ${suCo.kinhDo}`;

  return `
            <div class="col-12 mb-2" id="suco-card-${suCo.id}" data-lat="${suCo.viDo || ""}" data-lng="${suCo.kinhDo || ""}" data-json='${encodeURIComponent(JSON.stringify(suCo))}'>
              <div class="card card-sos">
                <div class="d-flex align-items-center w-100">
                  <span class="badge-pending me-3" style="min-width: 100px; text-align: center;">
                      ${suCo.tenLoaiSuCo || suCo.tenLoai || "Sự cố"}
                  </span>
                  <div class="flex-grow-1 d-flex align-items-center" style="gap:12px;">
                      <span class="text-truncate" style="max-width: 350px;" title="${displayAddress}">
                          <i class="fa-solid fa-map-pin text-secondary"></i>
                          <strong>Địa chỉ:</strong> ${displayAddress}
                      </span>
                      <span class="text-truncate" style="max-width: 250px;">
                          <strong>Mô tả:</strong> ${suCo.moTa || "Không có"}
                          <i class="fa-solid fa-circle-info info-icon" title="Xem chi tiết" onclick="openSuCoDetail(${suCo.id})"></i>
                      </span>
                  </div>
                  <small class="text-muted ms-auto me-3">${time}</small>
                  <button class="btn btn-secondary btn-sm" onclick="doiTrangThaiSuCo(${suCo.id}, 'DANG_XU_LY')">Tiếp nhận xử lý</button>
                </div>
              </div>
            </div>`;
}
function loadPendingSuCo() {
  if (!TRUSO_ID || TRUSO_ID === 0) return;

  fetch(`/api/map/su-co?idTruSo=${TRUSO_ID}`)
    .then((res) => res.json())
    .then((data) => {
      console.log("Dữ liệu nhận được:", data);
      const list = document.getElementById("sos-list");
      list.innerHTML = "";

      // LỌC: Chỉ giữ lại những sự cố có trạng thái CHO_XU_LY
      const pendingData = data.filter((item) => {
        // Kiểm tra thuộc tính từ SuCoMapDto
        return item.trangThaiXuLy === "CHO_XU_LY";
      });

      const noDataElem = document.getElementById("no-data");
      if (!pendingData || pendingData.length === 0) {
        if (noDataElem) {
          noDataElem.style.display = "block";
          noDataElem.innerHTML = "Hiện không có sự cố nào đang chờ đề xuất!";
        }
        return;
      }

      if (noDataElem) noDataElem.style.display = "none";

      // Sắp xếp mới nhất lên đầu
      pendingData.sort((a, b) => b.id - a.id);

      pendingData.forEach((suCo) => {
        list.innerHTML += renderSuCoItem(suCo);
      });
    })
    .catch((err) => console.error("Lỗi tải sự cố:", err));
}
// Toggle between views: 'sos' or 'suco'
function setActiveView(view) {
  CURRENT_VIEW = view;
  // Dọn dẹp tất cả các bộ đếm ngược đang chạy khi chuyển view
  Object.keys(demNguocIntervals).forEach((id) => dungDemNguocInline(id));
  Object.keys(demNguocSOS).forEach((id) => dungDemNguoc(id));
  const btnSuCo = document.getElementById("btn-su-co");
  const btnSos = document.getElementById("btn-sos");
  const list = document.getElementById("sos-list");

  // Reset danh sách hiển thị
  list.innerHTML =
    '<div class="text-center p-5"><i class="fa-solid fa-spinner fa-spin fa-2x"></i><p>Đang tải dữ liệu...</p></div>';
  if (view === "suco") {
    btnSuCo.classList.remove("btn-outline-secondary");
    btnSuCo.classList.add("btn-secondary");
    btnSos.classList.remove("btn-danger");
    btnSos.classList.add("btn-outline-danger");
    // load sự cố
    loadPendingSuCo();
  } else {
    // default to sos
    btnSos.classList.remove("btn-outline-danger");
    btnSos.classList.add("btn-danger");
    btnSuCo.classList.remove("btn-secondary");
    btnSuCo.classList.add("btn-outline-secondary");
    // load sos
    const list = document.getElementById("sos-list");
    list.innerHTML = "";
    loadPendingSOS();
  }
}

// Handle realtime messages for SỰ CỐ
function handleRealtimeSuCo(suCo) {
  const id = suCo.id;
  const status = suCo.trangThaiXuLy || suCo.status;
  const card = document.getElementById("suco-card-" + id);

  console.log(">>> WebSocket nhận sự cố:", id, "Trạng thái:", status);

  // Nếu trạng thái KHÔNG PHẢI là "CHO_XU_LY", xóa nó khỏi danh sách chờ
  if (
    status === "HUY_BO" ||
    status === "DANG_XU_LY" ||
    status === "HOAN_THANH"
  ) {
    if (card) {
      card.remove();
      checkEmptyList();
    }
    return;
  }

  // Nếu là sự cố mới (CHO_XU_LY) và đang ở tab "Sự cố"
  if (status === "CHO_XU_LY" && CURRENT_VIEW === "suco") {
    if (!card) {
      const list = document.getElementById("sos-list");
      const noData = document.getElementById("no-data");
      if (noData) noData.style.display = "none";
      if (list) {
        list.insertAdjacentHTML("afterbegin", renderSuCoItem(suCo));
      }
    }
  }
}

function handleRealtimeSOS(sos) {
  const status = sos.trangThai || sos.status;

  if (status === "CHO_XU_LY" && CURRENT_VIEW === "sos") {
    const list = document.getElementById("sos-list");
    const cardExist = document.getElementById("sos-card-" + sos.id);

    // Chỉ thêm nếu card này chưa tồn tại trong danh sách
    if (!cardExist) {
      const noData = document.getElementById("no-data");
      if (noData) noData.style.display = "none";

      // Chèn vào đầu danh sách
      list.insertAdjacentHTML("afterbegin", renderSOSItem(sos));

      // Kích hoạt đếm ngược cho card mới
      batDauDemNguocInline(sos.id, 60);

      // Phát âm thanh
      const audio = new Audio("/assets/sounds/alert.mp3");
      audio.play().catch((e) => {
        // Thay vì chỉ console.log, bạn có thể hiển thị một Toast thông báo
        // để người dùng click vào, từ đó "kích hoạt" quyền phát âm thanh.
        console.warn(
          "Âm thanh bị chặn bởi trình duyệt. Cần tương tác người dùng.",
        );
      });
    }
  } else if (status !== "CHO_XU_LY") {
    // Nếu trạng thái thay đổi sang cái khác, xóa card đó đi
    const card = document.getElementById("sos-card-" + sos.id);
    if (card) card.remove();
    checkEmptyList();
  }
}
function checkEmptyList() {
  const list = document.getElementById("sos-list");
  // Đếm số lượng card thực tế còn lại (không tính div no-data)
  const cards = list.querySelectorAll(
    '.col-12[id^="sos-card-"], .col-12[id^="suco-card-"]',
  );

  if (cards.length === 0) {
    const noData = document.getElementById("no-data");
    if (noData) {
      noData.style.display = "block";
      noData.innerHTML =
        CURRENT_VIEW === "sos"
          ? '<i class="fa-solid fa-circle-check text-success fa-3x mb-3"></i><p>Hiện không có yêu cầu cứu trợ nào đang chờ!</p>'
          : '<i class="fa-solid fa-circle-check text-success fa-3x mb-3"></i><p>Hiện không có sự cố nào đang chờ!</p>';
    }
  }
}
// STOMP realtime subscription
// Lưu trữ thông tin đếm ngược cho mỗi SOS
var demNguocSOS = {};

function connectRealtime() {
  if (!TRUSO_ID || TRUSO_ID === 0) return;
  const socket = new SockJS("/ws-suco");
  const stompClient = Stomp.over(socket);
  stompClient.connect(
    {},
    function (frame) {
      // SOS channel
      stompClient.subscribe("/topic/truso/" + TRUSO_ID, function (message) {
        try {
          const sos = JSON.parse(message.body);
          handleRealtimeSOS(sos);
        } catch (e) {
          console.error("Invalid SOS message", e);
        }
      });

      // Kênh điều phối - nhận thông tin chuyển tiếp SOS
      stompClient.subscribe(
        "/topic/truso/" + TRUSO_ID + "/dieu-phoi",
        function (message) {
          try {
            const thongBao = JSON.parse(message.body);
            xuLyThongBaoDieuPhoi(thongBao);
          } catch (e) {
            console.error("Lỗi xử lý thông báo điều phối", e);
          }
        },
      );

      // Sự cố channel (matches trang-chu subscription)
      stompClient.subscribe(
        "/topic/tru-so/" + TRUSO_ID + "/su-co",
        function (message) {
          try {
            const suco = JSON.parse(message.body);
            handleRealtimeSuCo(suco);
          } catch (e) {
            console.error("Invalid su-co message", e);
          }
        },
      );
    },
    function (err) {
      console.error("STOMP error", err);
    },
  );
}

// Xử lý thông báo điều phối từ server
function xuLyThongBaoDieuPhoi(thongBao) {
  const loaiThongBao = thongBao.loaiThongBao;
  const idSos = thongBao.idSos;

  if (loaiThongBao === "SOS_MOI") {
    // SOS mới được điều phối đến trụ sở này
    const viTri = thongBao.viTriTrongDanhSach;
    const tongSo = thongBao.tongSoTruSo;
    const thoiGianCho = thongBao.thoiGianConLai || 60;

    console.log(`[ĐIỀU PHỐI] Nhận SOS #${idSos} - Vị trí ${viTri}/${tongSo}`);

    // Bắt đầu đếm ngược với thời gian từ server
    batDauDemNguocInline(idSos, thoiGianCho);

    // Bắt đầu đếm ngược badge
    batDauDemNguoc(idSos, thoiGianCho, viTri, tongSo);

    // Hiển thị thông báo
    hienThiThongBaoSOS(idSos, viTri, tongSo);
  } else if (loaiThongBao === "XOA_SOS") {
    // SOS đã được chuyển tiếp hoặc tiếp nhận bởi trụ sở khác
    const lyDo = thongBao.lyDo;
    console.log(`[ĐIỀU PHỐI] Xóa SOS #${idSos} - Lý do: ${lyDo}`);

    // Dừng đếm ngược
    dungDemNguoc(idSos);
    dungDemNguocInline(idSos);

    // Xóa card khỏi danh sách
    xoaCardSOS(idSos, lyDo);
  }
}

// ========== ĐẾM NGƯỢC INLINE (hiển thị trên mỗi card) ==========
function batDauDemNguocInline(idSos, thoiGianGiay) {
  // Dừng đếm ngược cũ nếu có
  dungDemNguocInline(idSos);

  let thoiGianConLai = thoiGianGiay;
  const timerElem = document.getElementById("timer-sos-" + idSos);
  if (!timerElem) return;

  // Cập nhật hiển thị ban đầu
  capNhatHienThiTimer(timerElem, thoiGianConLai);

  demNguocIntervals[idSos] = setInterval(() => {
    thoiGianConLai--;
    capNhatHienThiTimer(timerElem, thoiGianConLai);

    if (thoiGianConLai <= 0) {
      dungDemNguocInline(idSos);
      // Tự động gọi từ chối khi hết giờ
      tuChoiTiepNhan(idSos, true);
    }
  }, 1000);
}

function dungDemNguocInline(idSos) {
  if (demNguocIntervals[idSos]) {
    clearInterval(demNguocIntervals[idSos]);
    delete demNguocIntervals[idSos];
  }
}

function capNhatHienThiTimer(timerElem, giay) {
  if (!timerElem) return;

  const phut = Math.floor(giay / 60);
  const giayConLai = giay % 60;
  const thoiGianStr =
    phut > 0 ? `${phut}:${giayConLai.toString().padStart(2, "0")}` : `${giay}s`;

  timerElem.innerHTML = `<i class="fa-solid fa-clock"></i> ${thoiGianStr}`;

  // Đổi màu theo thời gian còn lại
  timerElem.classList.remove("warning", "danger");
  if (giay <= 10) {
    timerElem.classList.add("danger");
  } else if (giay <= 30) {
    timerElem.classList.add("warning");
  }
}

// ========== TỪ CHỐI TIẾP NHẬN ==========
function tuChoiTiepNhan(idSos, tuDong = false) {
  if (
    !tuDong &&
    !confirm("Bạn chắc chắn không muốn tiếp nhận tín hiệu SOS này?")
  ) {
    return;
  }

  const btnDecline = document.getElementById("btn-decline-" + idSos);
  const btnAccept = document.getElementById("btn-accept-" + idSos);
  if (btnDecline) {
    btnDecline.disabled = true;
    btnDecline.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';
  }
  if (btnAccept) {
    btnAccept.disabled = true;
  }

  // Dừng đếm ngược
  dungDemNguocInline(idSos);
  dungDemNguoc(idSos);

  fetch(`/api/tin-hieu-sos/tu-choi/${idSos}?idTruSo=${TRUSO_ID}`, {
    method: "POST",
  })
    .then((res) => res.json())
    .then((data) => {
      console.log("[TỪ CHỐI] Kết quả:", data);

      // Xóa card khỏi danh sách với hiệu ứng
      const card = document.getElementById("sos-card-" + idSos);
      if (card) {
        card.style.transition = "opacity 0.3s, transform 0.3s";
        card.style.opacity = "0";
        card.style.transform = "translateX(100%)";

        setTimeout(() => {
          card.remove();
          checkEmptyList();
        }, 300);
      }

      // Thông báo cho người dùng
      if (tuDong) {
        console.log(
          `⏰ Đã hết 60 giây - SOS #${idSos} được chuyển cho trụ sở tiếp theo`,
        );
      } else {
        // Không dùng alert để tránh gián đoạn
        console.log(`✓ Đã từ chối tiếp nhận SOS #${idSos}`);
      }
    })
    .catch((err) => {
      console.error("[LỖI] Không thể từ chối:", err);
      alert("Có lỗi xảy ra. Vui lòng thử lại.");

      // Khôi phục nút
      if (btnDecline) {
        btnDecline.disabled = false;
        btnDecline.innerHTML =
          '<i class="fa-solid fa-xmark"></i> Không tiếp nhận';
      }
      if (btnAccept) {
        btnAccept.disabled = false;
      }
    });
}

// Bắt đầu đếm ngược cho SOS (badge phiên bản cũ - giữ lại để tương thích)
function batDauDemNguoc(idSos, thoiGianGiay, viTri, tongSo) {
  // Dừng đếm ngược cũ nếu có
  dungDemNguoc(idSos);

  demNguocSOS[idSos] = {
    thoiGianConLai: thoiGianGiay,
    viTri: viTri,
    tongSo: tongSo,
    interval: setInterval(() => {
      demNguocSOS[idSos].thoiGianConLai--;
      capNhatHienThiDemNguoc(idSos);

      if (demNguocSOS[idSos].thoiGianConLai <= 0) {
        dungDemNguoc(idSos);
      }
    }, 1000),
  };

  // Cập nhật hiển thị ngay lập tức
  capNhatHienThiDemNguoc(idSos);
}

// Dừng đếm ngược
function dungDemNguoc(idSos) {
  if (demNguocSOS[idSos] && demNguocSOS[idSos].interval) {
    clearInterval(demNguocSOS[idSos].interval);
    delete demNguocSOS[idSos];
  }
}

// Cập nhật hiển thị đếm ngược trên card
function capNhatHienThiDemNguoc(idSos) {
  const card = document.getElementById("sos-card-" + idSos);
  if (!card) return;

  const thongTin = demNguocSOS[idSos];
  if (!thongTin) return;

  let demNguocElem = card.querySelector(".dem-nguoc-badge");
  if (!demNguocElem) {
    // Tạo element đếm ngược nếu chưa có
    demNguocElem = document.createElement("span");
    demNguocElem.className = "dem-nguoc-badge badge bg-danger ms-2";
    demNguocElem.style.cssText =
      "animation: pulse 1s infinite; font-size: 0.9rem;";

    const cardBody = card.querySelector(".card-sos");
    if (cardBody) {
      cardBody.insertBefore(demNguocElem, cardBody.firstChild);
    }
  }

  const phut = Math.floor(thongTin.thoiGianConLai / 60);
  const giay = thongTin.thoiGianConLai % 60;
  const thoiGianStr = `${phut}:${giay.toString().padStart(2, "0")}`;

  demNguocElem.innerHTML = `<i class="fa-solid fa-clock"></i> ${thoiGianStr} <small>(${thongTin.viTri}/${thongTin.tongSo})</small>`;

  // Đổi màu khi sắp hết thời gian
  if (thongTin.thoiGianConLai <= 10) {
    demNguocElem.classList.remove("bg-danger", "bg-warning");
    demNguocElem.classList.add("bg-danger");
    demNguocElem.style.animation = "pulse 0.5s infinite";
  } else if (thongTin.thoiGianConLai <= 30) {
    demNguocElem.classList.remove("bg-danger");
    demNguocElem.classList.add("bg-warning", "text-dark");
  }
}

// Hiển thị thông báo khi có SOS mới
function hienThiThongBaoSOS(idSos, viTri, tongSo) {
  // Phát âm thanh cảnh báo
  try {
    const audio = new Audio("/assets/sounds/alert.mp3");
    audio.play().catch(() => {});
  } catch (e) {}

  // Có thể thêm toast notification ở đây
  console.log(`🚨 SOS mới! Bạn là trụ sở thứ ${viTri}/${tongSo} nhận tin này.`);
}

// Xóa card SOS khi bị chuyển tiếp
function xoaCardSOS(idSos, lyDo) {
  const card = document.getElementById("sos-card-" + idSos);
  if (card) {
    // Hiệu ứng bay sang phải và mờ dần
    card.style.transition = "all 0.4s ease";
    card.style.opacity = "0";
    card.style.transform = "translateX(50px)";

    setTimeout(() => {
      card.remove();
      checkEmptyList(); // Cập nhật lại giao diện "Hiện không có yêu cầu nào"

      // Thông báo nhanh (Toast) cho nhân viên trực trụ sở
      if (lyDo === "NGUOI_DUNG_HUY") {
        console.warn(`SOS #${idSos} đã bị người dùng hủy.`);
      }
    }, 400);
  }
}

/* Detail panel for list page + overlay */
(function insertDetailPanel() {
  const main = document.querySelector(".main-content");
  if (!main) return;

  // overlay
  const overlay = document.createElement("div");
  overlay.id = "overlay";
  overlay.style.display = "none";
  document.body.appendChild(overlay);

  const panel = document.createElement("div");
  panel.id = "detail-panel";
  panel.style.overflow = "auto";
  panel.style.background = "#fff";
  panel.style.border = "1px solid #e5e7eb";
  panel.style.boxShadow = "0 10px 25px rgba(0,0,0,0.08)";
  panel.style.borderRadius = "8px";
  panel.style.display = "none";
  panel.innerHTML = `
                <div style="padding:12px 16px; display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #eee;">
                  <strong>Chi tiết</strong>
                  <button id="detail-close" style="border:none;background:none;cursor:pointer;font-size:16px">✖</button>
                </div>
                <div id="detail-body" style="padding:12px"></div>
              `;
  document.body.appendChild(panel);

  const btn = document.getElementById("detail-close");
  if (btn) {
    btn.addEventListener("click", () => {
      panel.style.display = "none";
      overlay.style.display = "none";
    });
  }

  overlay.addEventListener("click", function () {
    panel.style.display = "none";
    overlay.style.display = "none";
  });
})();

function openSOSDetail(id) {
  const el = document.getElementById("sos-card-" + id);
  if (!el) return;
  const raw = el.getAttribute("data-json");
  let obj = null;
  try {
    obj = JSON.parse(decodeURIComponent(raw));
  } catch (e) {
    console.error(e);
  }
  const panel = document.getElementById("detail-panel");
  const body = document.getElementById("detail-body");
  if (!panel || !body) return;
  if (!obj) {
    body.innerHTML = '<div class="text-muted">No data</div>';
  } else {
    const imgPath = fixUrl(obj.hinhAnh);
    const audioPath = fixUrl(obj.ghiAm);

    const img = imgPath
      ? `<img src="${imgPath}" style="width:100%;border-radius:6px;margin-bottom:8px;cursor:pointer" onclick="window.open(this.src)"/>`
      : '<p class="text-muted small">Không có ảnh hiện trường</p>';

    const audio = audioPath
      ? `<div class="mt-2">
                      <label class="small text-muted">Ghi âm khẩn cấp:</label>
                      <audio controls style="width:100%"><source src="${audioPath}" type="audio/mp4"></audio>
                     </div>`
      : '<p class="text-muted small">Không có ghi âm</p>';

    body.innerHTML = `
                  <div style="font-weight:700;margin-bottom:6px;color:#dc3545">TÍN HIỆU SOS #${obj.id}</div>
                  <div style="margin-bottom:6px"><strong>Người gửi:</strong> ${obj.user ? obj.user.name : "Ẩn danh"}</div>
      <div style="margin-bottom:6px"><strong>Địa chỉ:</strong> ${obj.diaChi || obj.viDo + ", " + obj.kinhDo}</div>            <div style="margin-bottom:6px"><strong>Ghi chú:</strong> ${obj.ghiChu || "Không có"}</div>
                  <div style="margin-bottom:12px"><strong>Thời gian:</strong> ${obj.createdAt ? new Date(obj.createdAt).toLocaleString("vi-VN") : ""}</div>
                  <hr>
                  ${img}
                  ${audio}
                  <div style="margin-top:15px">
                      <button class="btn btn-primary w-100" onclick="confirmRescue(${obj.id})">
                          <i class="fa-solid fa-truck-fast"></i> XÁC NHẬN CỨU TRỢ
                      </button>
                  </div>
              `;
  }
  panel.style.display = "block";
  const overlayElem = document.getElementById("overlay");
  if (overlayElem) overlayElem.style.display = "block";
}

function openSuCoDetail(id) {
  const el = document.getElementById("suco-card-" + id);
  if (!el) return;
  const raw = el.getAttribute("data-json");
  let obj = null;
  try {
    obj = JSON.parse(decodeURIComponent(raw));
  } catch (e) {
    console.error(e);
  }
  const panel = document.getElementById("detail-panel");
  const body = document.getElementById("detail-body");
  if (!panel || !body) return;
  if (!obj) {
    body.innerHTML = '<div class="text-muted">No data</div>';
  } else {
    const img = obj.hinhAnhUrl
      ? `<img src="${obj.hinhAnhUrl}" style="width:100%;border-radius:6px;margin-bottom:8px"/>`
      : "";
    body.innerHTML = `
                  <div style="font-weight:700;margin-bottom:6px">Sự cố #${
                    obj.id
                  }</div>
                  <div style="margin-bottom:6px"><strong>Loại:</strong> ${
                    obj.tenLoaiSuCo || obj.tenLoai || "N/A"
                  }</div>
                  <div style="margin-bottom:6px"><strong>Địa chỉ:</strong> ${obj.diaChi || obj.viDo + ", " + obj.kinhDo}</div>
                  <div style="margin-bottom:6px"><strong>Mô tả:</strong> ${
                    obj.moTa || "Không"
                  }</div>
                  <div style="margin-bottom:6px"><strong>Mức độ:</strong> ${
                    obj.mucDoNghiemTrong || obj.mucDo || "N/A"
                  }</div>
                  ${img}
                  <div style="margin-top:8px"><button class="btn btn-secondary" onclick="doiTrangThaiSuCo(${
                    obj.id
                  }, 'DANG_XU_LY')">Tiếp nhận xử lý</button></div>
                `;
  }
  panel.style.display = "block";
  const overlayElem2 = document.getElementById("overlay");
  if (overlayElem2) overlayElem2.style.display = "block";
}

function doiTrangThaiSuCo(id, status) {
  if (!confirm("Xác nhận tiếp nhận xử lý sự cố này?")) return;

  // Giao diện chờ
  const btn = event.target; // Lấy nút vừa bấm
  if (btn) {
    btn.disabled = true;
    btn.innerText = "Đang xử lý...";
  }

  fetch(`/su-co/cap-nhat-trang-thai/${id}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      status: status,
    }),
  })
    .then((res) => {
      if (!res.ok) throw new Error("Lỗi Server: " + res.status);
      return res.json().catch(() => ({}));
    })
    .then(() => {
      alert("Tiếp nhận thành công! Đang chuyển đến danh sách cứu trợ.");
      window.location.href = "/truso/dang-cuu-tro";
    })
    .catch((err) => {
      console.error("Lỗi:", err);
      alert("Không thể tiếp nhận. Vui lòng thử lại.");
      if (btn) {
        btn.disabled = false;
        btn.innerText = "Tiếp nhận xử lý";
      }
    });
}

setInterval(() => {
  document.getElementById("clock").innerText = new Date().toLocaleTimeString();
}, 1000);

function confirmRescue(id) {
  if (!confirm("Xác nhận tiếp nhận cứu trợ cho ca này?")) return;

  // Hiệu ứng chờ trên nút
  const btn = document.getElementById("btn-accept-" + id);
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Đang xử lý...';
  }

  // Gửi yêu cầu lên server
  fetch(`/api/tin-hieu-sos/tiep-nhan/${id}?idTruSo=${TRUSO_ID}`, {
    method: "POST",
  })
    .then((res) => res.json())
    .then((data) => {
      if (data.status === "success" || data.id) {
        // Chuyển hướng sang trang bản đồ để bắt đầu đi cứu trợ
        // Giả sử trang chủ của bạn có bản đồ và truyền tham số ID
        window.location.href = "/truso/trang-chu?focusSos=" + id;
      } else {
        alert("Lỗi: " + (data.message || "Không thể tiếp nhận ca này."));
        location.reload(); // Reload để cập nhật trạng thái mới nhất
      }
    })
    .catch((err) => {
      console.error(err);
      alert("Có lỗi kết nối hệ thống.");
    });
}

window.addEventListener("load", function () {
  // 1. Kiểm tra ID trụ sở ngay lập tức
  if (!TRUSO_ID || TRUSO_ID === 0) {
    console.error("Không tìm thấy ID trụ sở từ Session!");
    return;
  }

  // 2. Gán sự kiện cho các nút chuyển view (Tab)
  const btnSuCo = document.getElementById("btn-su-co");
  const btnSos = document.getElementById("btn-sos");
  if (btnSuCo) btnSuCo.addEventListener("click", () => setActiveView("suco"));
  if (btnSos) btnSos.addEventListener("click", () => setActiveView("sos"));

  // 3. Kết nối Realtime trước để không bỏ lỡ tin nhắn nào
  connectRealtime();

  // 4. Hiển thị view mặc định (Hàm này sẽ tự gọi loadPendingSOS)
  setActiveView("sos");
});
