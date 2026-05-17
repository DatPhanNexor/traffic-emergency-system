function fixUrl(path) {
  if (!path) return null;
  if (path.startsWith("http") || path.startsWith("/uploads/")) {
    return path;
  }
  return `/uploads/sos/${path}`;
}
/*<![CDATA[*/
var TRUSO_ID = /*[[${session.currentTruSo != null ? session.currentTruSo.id : 0}]]*/ 0;
/*]]>*/
let currentFilter = "su-co";
let allData = [];
function formatTime(iso) {
  try {
    const d = new Date(iso);
    return d.toLocaleString();
  } catch (e) {
    return iso;
  }
}
// 1. Sửa hàm render để hiện nút Tạo thanh toán
function renderRescueItem(sos) {
  // Debug: Kiểm tra ID
  if (!sos.id) {
    console.error("renderRescueItem: Missing ID!", sos);
  }

  const displayLocation =
    sos._raw?.diaChi || `${sos.viDo || ""}, ${sos.kinhDo || ""}`;
  const points = sos.userPoints || 0;

  // --- SỬA LẠI LOGIC NÚT GỌI TẠI ĐÂY ---
  // Lấy Firebase UID (chuỗi) thay vì ID số để Android có thể nhận diện hòm thư
  const userUid = sos.reporterUid;

  let callBtn = "";
  if (userUid) {
    callBtn = `
          <button class="btn btn-sm btn-primary mb-2 w-100" onclick="startCallWithUser('${userUid}')">
              <i class="fa-solid fa-phone"></i> Gọi cho khách
          </button>`;
  } else {
    callBtn = `
          <button class="btn btn-sm btn-outline-secondary mb-2 w-100" disabled>
              <i class="fa-solid fa-user-slash"></i> Ẩn danh
          </button>`;
  }
  // ------------------------------------

  // Logic Hóa đơn & Trạng thái (Giữ nguyên của bạn)
  let actionBtn = "";
  if (sos.hoaDon) {
    if (sos.hoaDon.trangThai === "PAID") {
      actionBtn = `<button class="btn btn-sm btn-primary w-100" onclick="completeRescue(${sos.id || 0}, '${sos.itemType}')">
                          <i class="fa-solid fa-check-double"></i> Hoàn thành
                       </button>`;
    } else {
      actionBtn = `<button class="btn btn-sm btn-outline-secondary w-100" disabled>
                          <i class="fa-solid fa-hourglass-half fa-spin"></i> Đang đợi trả tiền
                       </button>`;
    }
  } else {
    if (sos.itemType !== "SUCO") {
      actionBtn = `<button class="btn btn-sm btn-success w-100" onclick="openPayment(${sos.id || 0})">
                          <i class="fa-solid fa-file-invoice"></i> Tạo hóa đơn
                      </button>`;
    } else {
      // Đối với Sự cố, hiện luôn nút Hoàn thành vì không cần thanh toán
      actionBtn = `<button class="btn btn-sm btn-primary w-100" onclick="completeRescue(${sos.id || 0}, '${sos.itemType}')">
                          <i class="fa-solid fa-check"></i> Hoàn thành
                       </button>`;
    }
  }

  const totalActions = callBtn + actionBtn;

  return `
      <div class="card-sos" id="rescue-${sos.id}" data-json='${encodeURIComponent(JSON.stringify(sos))}'>
          <span class="badge bg-warning text-dark me-2">
              <i class="fa-solid fa-star"></i> ${points}
              ${sos.isVip ? '<i class="fa-solid fa-crown text-danger ms-1"></i>' : ""}
          </span>
          <div class="flex-grow-1 d-flex flex-column" style="overflow: hidden;">
              <div class="text-truncate"><strong>Vị trí:</strong> ${displayLocation}</div>
              <div class="text-truncate text-muted small">
                  <strong>Ghi chú:</strong> ${sos.ghiChu}
                  <i class="fa-solid fa-circle-info ms-1" style="cursor: pointer; color: #0d6efd;" onclick="openDetailFromRow(${sos.id})"></i>
              </div>
          </div>
          <div class="ms-auto d-flex flex-column align-items-end" style="min-width: 140px;">
              ${totalActions}
          </div>
      </div>`;
}
async function completeRescue(id, type) {
  console.log("completeRescue called - ID:", id, "Type:", type);

  if (!id || id === 0) {
    alert("Lỗi: Không tìm thấy ID của yêu cầu cứu trợ!");
    console.error("Invalid ID:", id);
    return;
  }

  if (!confirm("Xác nhận đã hoàn thành cứu trợ và đóng yêu cầu này?")) return;

  try {
    // Chọn đúng API endpoint tùy theo loại (SOS hoặc Sự cố)
    const url =
      type === "SOS"
        ? `/api/tin-hieu-sos/cap-nhat-trang-thai/${id}`
        : `/su-co/cap-nhat-trang-thai/${id}`;

    console.log("Calling API:", url);

    const res = await fetch(url, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        status: "HOAN_THANH", // Trạng thái mới sau khi hoàn thành
      }),
    });

    if (res.ok) {
      // Cập nhật mảng local để item biến mất ngay lập tức nhờ hàm filter trong renderData
      const idx = allData.findIndex(
        (it) => it.id === id && it.itemType === type,
      );
      if (idx !== -1) {
        allData[idx].trangThaiXuLy = "HOAN_THANH"; // Trạng thái này sẽ bị filter loại bỏ
        renderData(); // Vẽ lại list
      }
      alert("Đã hoàn tất ca cứu trợ!");
    } else {
      const errorText = await res.text();
      alert(`Lỗi khi cập nhật: ${res.status} - ${errorText}`);
      console.error("API Error:", res.status, errorText);
    }
  } catch (e) {
    console.error("Lỗi:", e);
    alert("Đã xảy ra lỗi: " + e.message);
  }
}
// 2. Các hàm xử lý Form Thanh toán mới
let currentSosId = null;

function openPayment(id) {
  const el = document.getElementById("rescue-" + id);
  const data = JSON.parse(decodeURIComponent(el.getAttribute("data-json")));
  currentSosId = id;

  const amountInput = document.getElementById("pay-amount");
  const statusArea = document.getElementById("status-area");
  const vipNote = document.getElementById("vip-note");

  if (data.isVip) {
    statusArea.innerHTML = `<span class="vip-badge"><i class="fa-solid fa-crown"></i> Khách hàng VIP (Hệ thống tự tính tiền)</span>`;
    amountInput.value = 0;
    amountInput.readOnly = true;
    vipNote.innerText = "Giá 0đ nếu < hạn mức km. Backend sẽ tự xử lý.";
  } else {
    statusArea.innerHTML = `<span class="normal-badge"><i class="fa-solid fa-user"></i> Khách hàng vãng lai</span>`;
    amountInput.value = "";
    amountInput.readOnly = false;
    vipNote.innerText = "Vui lòng nhập giá thỏa thuận với khách.";
  }

  document.getElementById("overlay").style.display = "block";
  document.getElementById("payment-panel").style.display = "block";
}

function closePayment() {
  document.getElementById("payment-panel").style.display = "none";
  document.getElementById("overlay").style.display = "none";
}

async function submitPayment(e) {
  if (e) e.preventDefault();

  const payload = {
    sosId: currentSosId,
    tenSos: document.getElementById("pay-sos-name").value,
    xuLy: document.getElementById("pay-treatment").value,
    giaThuCong: document.getElementById("pay-amount").value,
    trusoId: TRUSO_ID,
  };

  try {
    const res = await fetch("/api/hoa-don/tao", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (res.ok) {
      const hoaDonMoi = await res.json();
      alert("Đã tạo hóa đơn thành công!");
      closePayment();

      // SỬA TẠI ĐÂY: Tìm theo ID mà không phân biệt SOS hay SUCO
      const idx = allData.findIndex((it) => it.id === currentSosId);

      if (idx !== -1) {
        // Cập nhật dữ liệu để renderData() nhận diện được
        allData[idx].hoaDon = hoaDonMoi;
        // Ép trạng thái về PENDING để logic hiển thị nút "Đang đợi trả tiền" kích hoạt
        allData[idx].trangThaiXuLy = "PENDING";

        console.log("Cập nhật Local thành công cho đơn #" + currentSosId);
        renderData(); // Vẽ lại giao diện ngay lập tức
      }

      // Tải lại để đảm bảo đồng bộ với Database
      loadActiveRescues();
    }
  } catch (err) {
    console.error("Lỗi gửi hóa đơn:", err);
  }
}

async function loadActiveRescues() {
  if (!TRUSO_ID || TRUSO_ID === 0) return;

  try {
    const [sosRes, sucoRes] = await Promise.all([
      fetch("/api/sos-cua-toi"),
      fetch("/su-co/danh-sach-hien-tai"),
    ]);

    if (!sosRes.ok && !sucoRes.ok) {
      throw new Error("Không thể lấy dữ liệu đang cứu trợ");
    }

    const sosData = sosRes.ok ? await sosRes.json() : [];
    const sucoData = sucoRes.ok ? await sucoRes.json() : [];

    console.log("RAW SOS API:", sosData);
    console.log("RAW SUCO API:", sucoData);

    const formattedSos = (sosData || []).map((s) => formatItem(s, "SOS"));
    const formattedSuCo = (sucoData || []).map((s) => formatItem(s, "SUCO"));

    allData = [...formattedSuCo, ...formattedSos].filter(
      (item, index, arr) =>
        arr.findIndex(
          (other) => other.id === item.id && other.itemType === item.itemType,
        ) === index,
    );

    console.log("FORMATTED allData:", allData);

    renderData();
  } catch (err) {
    console.error("Lỗi khi tải dữ liệu mới:", err);
  }
}
// Hàm phụ để chuẩn hóa dữ liệu cho sạch code
function formatItem(s, type) {
  // Ưu tiên lấy trangThai (của SOS) hoặc trangThaiXuLy (của Sự cố)
  let statusRaw =
    type === "SOS"
      ? s.trangThai || "DANG_XU_LY"
      : s.trangThaiXuLy || "DANG_XU_LY";

  return {
    id: s.id,
    viDo: s.viDo,
    kinhDo: s.kinhDo,
    ghiChu: type === "SOS" ? s.ghiChu || "SOS" : s.moTa || "Sự cố",
    // Ép kiểu chuẩn xác để hàm filter không bị lỗi
    trangThaiXuLy: String(statusRaw).toUpperCase().trim(),
    itemType: type,
    isVip: Boolean(s.isVip || s.vip),
    createdAt: s.createdAt || new Date().toISOString(),
    userPoints: s.user ? s.user.totalPoints : 0,
    // Quan trọng: Phải lấy được hoaDon từ object gốc s
    hoaDon: s.hoaDon || null,
    reporterUid: s.reporterUid,
    _raw: s,
  };
}
function filterType(type) {
  currentFilter = type;
  const btnSuCo = document.getElementById("btn-filter-su-co");
  const btnSOS = document.getElementById("btn-filter-sos");
  if (type === "su-co") {
    btnSuCo.classList.add("active");
    btnSOS.classList.remove("active");
  } else {
    btnSOS.classList.add("active");
    btnSuCo.classList.remove("active");
  }
  renderData();
}

function renderData() {
  const list = document.getElementById("rescue-list");
  let noDataElem = document.getElementById("no-data");
  if (!list) return;

  list.querySelectorAll(".rescue-card").forEach((e) => e.remove());
  let filtered = allData.filter((item) => {
    console.log("ITEM:", item);

    if (currentFilter) {
      if (currentFilter === "sos" && item.itemType !== "SOS") {
        return false;
      }

      if (currentFilter === "su-co" && item.itemType !== "SUCO") {
        return false;
      }
    }

    const st = String(item.trangThaiXuLy || "")
      .toUpperCase()
      .trim();

    console.log("STATUS:", st);

    const activeStates = ["CHO_XU_LY", "DANG_XU_LY", "PENDING"];

    return activeStates.includes(st);
  });
  console.log("CURRENT FILTER:", currentFilter);
  console.log("FILTERED DATA:", filtered);
  // Sắp xếp (giữ nguyên logic của bạn)
  filtered.sort((a, b) => {
    if (a.isVip !== b.isVip) return a.isVip ? -1 : 1;
    return new Date(b.createdAt) - new Date(a.createdAt);
  });

  if (filtered.length === 0) {
    // Kiểm tra an toàn trước khi dùng .style
    if (noDataElem) {
      noDataElem.style.display = "block";
      list.appendChild(noDataElem);
    } else {
      // Nếu lỡ bị xóa mất thì tạo lại nhanh
      list.innerHTML = `<div class="alert alert-secondary py-4" id="no-data">Hiện không có cứu trợ đang diễn ra.</div>`;
    }
  } else {
    if (noDataElem) noDataElem.style.display = "none";
    filtered.forEach((it) => {
      const wrapper = document.createElement("div");
      wrapper.innerHTML = renderRescueItem(it);
      list.appendChild(wrapper.firstElementChild);
    });
  }
}

function openDetailFromRow(id) {
  const el = document.getElementById("rescue-" + id);
  if (!el) return;
  const raw = el.getAttribute("data-json");
  let obj = null;
  try {
    obj = JSON.parse(decodeURIComponent(raw));
  } catch (e) {
    console.error(e);
  }

  const body = document.getElementById("detail-body");
  if (!obj) {
    body.innerHTML = '<div class="text-muted">Không có dữ liệu</div>';
  } else {
    const realData = obj._raw || {};
    const imgPath = fixUrl(realData.hinhAnh || realData.hinhAnhUrl);
    const audioPath = fixUrl(realData.ghiAm);

    // Lấy địa chỉ hiển thị trong chi tiết
    const fullAddress = realData.diaChi || `${obj.viDo}, ${obj.kinhDo}`;

    const imgHtml = imgPath
      ? `<img src="${imgPath}" class="img-fluid rounded mb-3" style="width:100%; cursor:zoom-in; border:1px solid #ddd;" onclick="window.open(this.src)">`
      : '<p class="text-muted small">Không có hình ảnh</p>';

    const audioHtml = audioPath
      ? `<div class="mb-3">
            <label class="small fw-bold">Ghi âm hiện trường:</label>
            <audio controls style="width:100%"><source src="${audioPath}" type="audio/mp4"></audio>
           </div>`
      : "";

    body.innerHTML = `
        <div class="mb-3">
            <h5 class="text-primary mb-0">${obj.itemType === "SOS" ? "🆘 SOS Khẩn cấp" : "⚠️ Sự cố"} #${obj.id}</h5>
            <small class="text-muted">${formatTime(obj.createdAt)}</small>
        </div>
        <div class="mb-2 bg-light p-2 rounded">
            <i class="fa-solid fa-location-dot text-danger"></i> <strong>Địa chỉ:</strong><br>
            <span class="text-dark">${fullAddress}</span>
        </div>
        <div class="mb-3 small"><strong>Nội dung:</strong> ${obj.ghiChu}</div>
        <hr>
        ${imgHtml}
        ${audioHtml}
        <div class="mt-3">
            <button class="btn btn-outline-success w-100 mb-2" onclick="gotoMap(${obj.id})">
                <i class="fa-solid fa-map-location-dot"></i> Dẫn đường trên bản đồ
            </button>
        </div>`;
  }
  document.getElementById("overlay").style.display = "block";
  document.getElementById("detail-panel").style.display = "block";
}

function gotoMap(id) {
  const el = document.getElementById("rescue-" + id);
  if (!el) return;
  const raw = el.getAttribute("data-json");
  let obj = null;
  try {
    obj = JSON.parse(decodeURIComponent(raw));
  } catch (e) {}
  const lat = obj ? obj.viDo : null;
  const lng = obj ? obj.kinhDo : null;
  if (lat && lng) {
    window.location.href = `/truso/trang-chu?toLat=${encodeURIComponent(
      lat,
    )}&toLng=${encodeURIComponent(lng)}&sosId=${id}`;
  } else {
    window.location.href = "/truso/trang-chu?sosId=" + id;
  }
}

document.getElementById &&
  document.addEventListener("click", function (e) {
    if (e.target && e.target.id === "detail-close") {
      document.getElementById("detail-panel").style.display = "none";
      document.getElementById("overlay").style.display = "none";
    }
  });

setInterval(() => {
  const c = document.getElementById("clock");
  if (c) c.innerText = new Date().toLocaleTimeString();
}, 1000);

// --- THÊM BIẾN VÀ HÀM KẾT NỐI SOCKET ---
let stompClient = null;

function connectWebSocket() {
  const socket = new SockJS("/ws-suco"); // Khớp với endpoint config ở Backend
  stompClient = Stomp.over(socket);

  // Tắt log debug rác trên console (tùy chọn)
  stompClient.debug = null;

  stompClient.connect(
    {},
    function (frame) {
      console.log("Connected to WebSocket: " + frame);

      // Lắng nghe topic của Trụ sở này
      // Backend bắn: messagingTemplate.convertAndSend("/topic/truso/" + hd.getTrusoId(), response);
      stompClient.subscribe(
        "/topic/truso/" + TRUSO_ID,
        function (messageOutput) {
          const data = JSON.parse(messageOutput.body);
          console.log("📩 Nhận tín hiệu từ Socket:", data);

          // Nếu nhận được gói tin có trạng thái PAID (thanh toán thành công)
          if (data.trangThai === "PAID") {
            updateLocalDataAfterPaid(data);
          }
        },
      );
      setupCallSubscription();
    },
    function (error) {
      console.error("Socket Error, retrying in 5s...", error);
      setTimeout(connectWebSocket, 5000); // Tự kết nối lại nếu rớt mạng
    },
  );
}

// Hàm cập nhật nhanh giao diện mà không cần load lại toàn bộ API
function updateLocalDataAfterPaid(paymentInfo) {
  // Tìm trong toàn bộ data, không phân biệt SOS hay SUCO, miễn là trùng ID
  const idx = allData.findIndex((it) => it.id === paymentInfo.sosId);

  if (idx !== -1) {
    // Cập nhật object hoaDon
    if (!allData[idx].hoaDon) allData[idx].hoaDon = {};
    allData[idx].hoaDon.trangThai = "PAID";
    allData[idx].hoaDon.tongThanhToan = paymentInfo.tongThanhToan;

    // Vẽ lại giao diện ngay lập tức
    renderData();
    console.log("🔔 Đã cập nhật trạng thái PAID cho đơn #" + paymentInfo.sosId);
  } else {
    // Nếu không tìm thấy trong cache, tốt nhất là load lại toàn bộ cho chắc
    loadActiveRescues();
  }
}

window.addEventListener("load", () => {
  // 1. Gán sự kiện cho nút bấm
  const bSuCo = document.getElementById("btn-filter-su-co");
  const bSOS = document.getElementById("btn-filter-sos");
  if (bSuCo) bSuCo.addEventListener("click", () => filterType("su-co"));
  if (bSOS) bSOS.addEventListener("click", () => filterType("sos"));

  // 2. Tải dữ liệu từ API (Hàm này sẽ tự gọi renderData sau khi xong)
  loadActiveRescues();
  connectWebSocket();
  // 3. Set trạng thái active cho nút ban đầu
  const initialBtn = currentFilter === "sos" ? bSOS : bSuCo;
  if (initialBtn) initialBtn.classList.add("active");
});

// --- CẤU HÌNH WEBRTC ---
let peerConnection = null;
let localStream = null;
let targetUserId = null;
const configuration = {
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }],
};

// Hàm khởi tạo cuộc gọi từ phía Web (Trụ sở gọi cho Android)
// Tìm hàm này và sửa lại tham số truyền vào
async function startCallWithUser(userUid) {
  // Đảm bảo userUid này là chuỗi kiểu: "3J9ua8nF4TQo2AE9Wo7ySlZkAuC2"
  targetUserId = userUid;
  showCallPanel("Đang gọi Android...");

  try {
    await initLocalStream();
    createPeerConnection();

    const offer = await peerConnection.createOffer();
    await peerConnection.setLocalDescription(offer);

    // Gửi signal với key "to" khớp với Backend String toUser = (String) signal.get("to");
    sendSignal({
      type: "OFFER",
      to: targetUserId, // Đây phải là UID chuỗi dài
      from: "TRU_SO_" + TRUSO_ID,
      sdp: offer.sdp,
    });
  } catch (e) {
    console.error("Lỗi khởi tạo gọi:", e);
    alert("Không thể truy cập Microphone hoặc trình duyệt không hỗ trợ!");
  }
}

// Hàm gửi tín hiệu qua Socket hiện có của bạn
function sendSignal(data) {
  if (stompClient && stompClient.connected) {
    console.log("📤 Sending WebRTC signal:", data.type, "to:", data.to);
    stompClient.send("/app/call-signal", {}, JSON.stringify(data));
  } else {
    console.error("❌ Socket chưa kết nối, không thể gửi tín hiệu!");
    alert("Kết nối máy chủ bị gián đoạn, vui lòng tải lại trang.");
  }
}

// Cập nhật hàm connectWebSocket hiện tại của bạn:
// Thêm phần subscribe cho topic cuộc gọi
function setupCallSubscription() {
  stompClient.subscribe(
    "/topic/tru-so/TRU_SO_" + TRUSO_ID + "/call",
    async (message) => {
      const signal = JSON.parse(message.body);
      const type = signal.type ? signal.type.toUpperCase() : "";

      console.log("📞 Tín hiệu nhận được:", type);

      if (type === "OFFER") {
        targetUserId = signal.from;
        handleOffer(signal);
      } else if (type === "ANSWER") {
        await peerConnection.setRemoteDescription(
          new RTCSessionDescription(signal),
        );
        document.getElementById("call-status").innerText = "Đang đàm thoại";
      } else if (type === "CANDIDATE") {
        if (peerConnection && signal.candidate) {
          try {
            await peerConnection.addIceCandidate(
              new RTCIceCandidate(signal.candidate),
            );
          } catch (e) {
            console.error("Lỗi thêm Candidate:", e);
          }
        }
      }
      // --- THÊM ĐOẠN NÀY ĐỂ XỬ LÝ KHI ANDROID TẮT ---
      else if (type === "BYE") {
        console.log("☎️ Android đã cúp máy.");
        cleanupCallLocally(); // Gọi hàm dọn dẹp giao diện và đóng kết nối
      }
    },
  );
}

// Xử lý khi Android gọi tới
async function handleOffer(offer) {
  showCallPanel("Cuộc gọi đến...");
  await initLocalStream();
  createPeerConnection();

  await peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
  const answer = await peerConnection.createAnswer();
  await peerConnection.setLocalDescription(answer);

  sendSignal({
    type: "ANSWER",
    to: targetUserId,
    from: "TRU_SO_" + TRUSO_ID,
    sdp: answer.sdp,
  });
  document.getElementById("call-status").innerText = "Đang đàm thoại";
}

function createPeerConnection() {
  if (peerConnection) peerConnection.close();
  peerConnection = new RTCPeerConnection(configuration);

  // Thêm track âm thanh vào kết nối
  localStream
    .getTracks()
    .forEach((track) => peerConnection.addTrack(track, localStream));

  // Nhận âm thanh từ Android
  peerConnection.ontrack = (event) => {
    const remoteAudio = document.getElementById("remoteAudio");
    if (remoteAudio) {
      // Đảm bảo remoteAudio không phát lại chính giọng của máy tính (nếu có nhầm lẫn stream)
      remoteAudio.srcObject = event.streams[0];

      // Mẹo: Đặt âm lượng mặc định vừa phải (0.8) để tránh mic máy tính thu lại tiếng loa quá to
      remoteAudio.volume = 0.8;

      remoteAudio.play().catch((err) => {
        console.error("Trình duyệt chặn phát:", err);
      });
    }
  };

  // Gửi ICE Candidate
  peerConnection.onicecandidate = (event) => {
    if (event.candidate) {
      sendSignal({
        type: "CANDIDATE",
        to: targetUserId,
        from: "TRU_SO_" + TRUSO_ID,
        // Đảm bảo gửi object phẳng để Android dễ parse
        candidate: {
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex,
        },
      });
    }
  };
}

async function initLocalStream() {
  if (!localStream) {
    try {
      localStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true, // Quan trọng nhất: Khử tiếng vọng
          noiseSuppression: true, // Giảm tiếng rè/ồn nền
          autoGainControl: true, // Tự điều chỉnh âm lượng mic
          googEchoCancellation: true, // Các thuộc tính riêng cho Chrome/Edge
          googNoiseSuppression: true,
          googHighpassFilter: true,
        },
        video: false,
      });
      console.log("✅ Local Stream initialized with Echo Cancellation");
    } catch (e) {
      console.error("❌ Không thể truy cập Mic:", e);
      throw e;
    }
  }
}

function showCallPanel(status) {
  document.getElementById("call-panel").style.display = "block";
  document.getElementById("call-status").innerText = status;
}

function endCall() {
  // 1. Gửi tín hiệu báo tử cho đối phương
  if (targetUserId) {
    sendSignal({
      type: "BYE",
      to: targetUserId,
      from: "TRU_SO_" + TRUSO_ID,
    });
  }

  // 2. Dọn dẹp tại chỗ
  if (peerConnection) {
    peerConnection.close();
    peerConnection = null;
  }
  if (localStream) {
    localStream.getTracks().forEach((t) => t.stop());
    localStream = null;
  }
  document.getElementById("call-panel").style.display = "none";
  targetUserId = null;
}

// Tạo hàm dọn dẹp riêng để dùng chung
function cleanupCallLocally() {
  if (peerConnection) peerConnection.close();
  if (localStream) localStream.getTracks().forEach((t) => t.stop());
  localStream = null;
  peerConnection = null;
  document.getElementById("call-panel").style.display = "none";
  targetUserId = null;
}

function toggleMic() {
  if (localStream) {
    const audioTrack = localStream.getAudioTracks()[0];
    audioTrack.enabled = !audioTrack.enabled;
    const btn = document.getElementById("btn-mic");
    btn.classList.toggle("btn-danger");
    btn.innerHTML = audioTrack.enabled
      ? '<i class="fa-solid fa-microphone"></i>'
      : '<i class="fa-solid fa-microphone-slash"></i>';
  }
}
