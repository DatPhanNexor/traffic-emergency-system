// --- KHỞI TẠO WEBSOCKET ---
const socket = new SockJS("/ws-suco");
const stompClient = Stomp.over(socket);
const cameraLoaded = new Set();
stompClient.connect({}, function (frame) {
  console.log("Connected to WebSocket");

  stompClient.subscribe("/topic/su-co", function (message) {
    const suCoDto = JSON.parse(message.body);
    const id = suCoDto.id;

    const cardPending = document.getElementById("pending-report-" + id);
    const tableRow = document.getElementById("report-" + id);

    console.log("Dữ liệu realtime:", suCoDto);

    // 1. CẬP NHẬT DÒNG TRONG BẢNG (Realtime hoàn toàn)
    if (tableRow) {
      // Cập nhật thuộc tính data-status để filter Tab không bị sai
      tableRow.setAttribute("data-status", suCoDto.trangThaiXuLy);

      // Cập nhật cột "Trụ sở tiếp nhận / Trạng thái" (Cột số 6)
      const statusCell = tableRow.cells[6];
      if (suCoDto.trangThaiXuLy === "CHO_XU_LY") {
        statusCell.innerHTML = `<span style="color: #f59e0b; font-style: italic">Đang chờ...</span>`;
      } else if (suCoDto.trangThaiXuLy === "DANG_XU_LY") {
        statusCell.innerHTML = `<span style="color: #3b82f6;">Trụ sở #${suCoDto.idTruSoTiepNhan || id} (Đang xử lý)</span>`;
      } else if (suCoDto.trangThaiXuLy === "HOAN_THANH") {
        statusCell.innerHTML = `<span style="color: #10b981;">Đã hoàn thành</span>`;
      } else if (
        suCoDto.trangThaiXuLy === "REJECTED" ||
        suCoDto.trangThaiDuyet === "REJECTED"
      ) {
        statusCell.innerHTML = `<span style="color: #ef4444;">Đã từ chối/Spam</span>`;
        tableRow.style.opacity = "0.6";
      }

      // Hiệu ứng highlight dòng vừa cập nhật
      tableRow.style.backgroundColor = "#f0f9ff";
      setTimeout(() => {
        tableRow.style.backgroundColor = "";
      }, 2000);
    }

    // 2. XỬ LÝ CARD TRÊN SLIDER
    const ketThuc =
      ["HUY_BO", "HOAN_THANH", "VERIFIED", "REJECTED"].includes(
        suCoDto.trangThaiXuLy,
      ) || ["VERIFIED", "REJECTED"].includes(suCoDto.trangThaiDuyet);

    if (ketThuc) {
      if (cardPending) {
        cardPending.remove();
        updateSliderVisibility(); // Cập nhật lại số lượng hiển thị slider
      }
    } else {
      // Cập nhật độ tin cậy nếu chưa kết thúc
      const updateConfidence = (parent) => {
        if (!parent) return;
        const confElem = parent.querySelector(".confidence-val");
        if (confElem) {
          confElem.innerText = suCoDto.doTinCay;
          confElem.style.color = "#ef4444";
          setTimeout(() => {
            confElem.style.color = "";
          }, 1000);
        }
      };
      if (cardPending) updateConfidence(cardPending);
      if (tableRow) updateConfidence(tableRow);
    }

    // 3. NẾU LÀ BÁO CÁO MỚI (AI duyệt xong)
    if (!cardPending && !tableRow && suCoDto.trangThaiDuyet === "AI_APPROVED") {
      renderNewCard(suCoDto); // Vẽ card ở trên
      renderNewRow(suCoDto); // VẼ DÒNG Ở DƯỚI (Thêm dòng này)

      if (typeof playNotificationSound === "function") playNotificationSound();
    }

    // 4. CẬP NHẬT LẠI CÁC CON SỐ TRÊN TAB
    updateTabCounts();
  });
});

function renderNewRow(suCoDto) {
  const tableBody = document.getElementById("table-body-main");
  if (!tableBody) return;

  // Kiểm tra nếu dòng này đã tồn tại thì không chèn nữa
  if (document.getElementById("report-" + suCoDto.id)) return;

  const row = document.createElement("tr");
  row.className = "incident-row";
  row.id = "report-" + suCoDto.id;
  row.setAttribute("data-lat", suCoDto.viDo);
  row.setAttribute("data-lng", suCoDto.kinhDo);
  row.setAttribute("data-status", suCoDto.trangThaiXuLy); // Thường là CHO_XU_LY

  row.innerHTML = `
        <td>${suCoDto.id}</td>
        <td><img src="${suCoDto.hinhAnhUrl || "/images/no-image.png"}" class="img-table" /></td>
        <td>${suCoDto.tenLoai || "Sự cố"}</td>
        <td>
            <div>${suCoDto.tenNguoiBao || "Người dân"}</div>
            <div style="font-size: 11px; color: #94a3b8">${suCoDto.emailNguoiBao || ""}</div>
        </td>
        <td>${suCoDto.diaChi || "Không rõ vị trí"}</td>
        <td class="confidence-val">${suCoDto.doTinCay || 1}</td>
        <td>
            <span style="color: #f59e0b; font-style: italic">Đang chờ...</span>
        </td>
        <td>
            <div id="cam-area-${suCoDto.id}" style="display: none; flex-direction: column; gap: 5px">
                <div id="cam-list-${suCoDto.id}" style="display: flex; gap: 5px; flex-wrap: wrap"></div>
            </div>
        </td>
    `;

  // Chèn lên đầu bảng
  tableBody.insertBefore(row, tableBody.firstChild);

  cameraLoaded.delete(suCoDto.id);
  // Sau khi chèn xong, quét camera cho dòng này luôn
  loadNearbyCameras(suCoDto.id);
}

function checkIfEmpty() {
  const container = document.querySelector(".card-container");
  // Nếu không còn card nào, load lại trang để Thymeleaf hiện div "Tạm thời không có báo cáo"
  if (container && container.querySelectorAll(".card").length === 0) {
    location.reload();
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
function renderNewCard(suCoDto) {
  const slider = document.getElementById("pending-slider");
  const noMsg = document.getElementById("no-pending-msg");

  if (noMsg) noMsg.style.display = "none";
  if (document.getElementById("pending-report-" + suCoDto.id)) return;

  const card = document.createElement("div");
  card.className = "card-mini pending-item new-card-highlight";
  card.id = "pending-report-" + suCoDto.id;

  // --- SỬA 1: Gán tọa độ để hàm loadNearbyCameras có dữ liệu để chạy ---
  card.setAttribute("data-lat", suCoDto.viDo);
  card.setAttribute("data-lng", suCoDto.kinhDo);

  // --- SỬA 2: Thêm div cam-list-pending để có chỗ hiển thị camera ---
  card.innerHTML = `
        <img src="${suCoDto.hinhAnhUrl || "/images/no-image.png"}" class="card-mini-img" />
        <div class="card-mini-body">
            <span style="color: var(--accent-color); font-weight: 700; font-size: 10px;">${suCoDto.tenLoai || "SỰ CỐ"}</span>
            <span><i class="fa-solid fa-user"></i> <b>${suCoDto.tenNguoiBao || "Người dân"}</b></span>
            <span class="text-truncate" title="${suCoDto.diaChi}">
                <i class="fa-solid fa-location-dot"></i> ${suCoDto.diaChi || "Không rõ vị trí"}
            </span>
            <span>Tin cậy: <b class="confidence-val">${suCoDto.doTinCay || 1}</b></span>
            
            <div id="cam-list-pending-${suCoDto.id}" style="display: flex; gap: 5px; margin-top: 5px"></div>

            <a href="javascript:void(0)" onclick="alert('Mô tả: ${suCoDto.moTa}')" style="margin-top: auto; color: #64748b">
                <i class="fa-solid fa-circle-info"></i> Chi tiết
            </a>
        </div>
        <div class="card-mini-actions">
            <button class="btn btn-ok" onclick="verify(${suCoDto.id}, true)">
                <i class="fa-solid fa-check"></i>
            </button>
            <button class="btn btn-fail" onclick="verify(${suCoDto.id}, false)">
                <i class="fa-solid fa-xmark"></i>
            </button>
        </div>
    `;

  slider.insertBefore(card, slider.firstChild);
  updateSliderVisibility();
}

async function verify(id, isCorrect) {
  if (!confirm("Xác nhận xử lý báo cáo này?")) return;
  try {
    const res = await fetch(
      `/admin/bao-cao-su-co/${id}/verify?isCorrect=${isCorrect}`,
      { method: "POST" },
    );

    if (res.ok) {
      // 1. Xóa Card ở Slider phía trên
      const pendingCard = document.getElementById("pending-report-" + id);
      if (pendingCard) pendingCard.remove();

      // 2. Xử lý dòng trong bảng
      const row = document.getElementById("report-" + id);
      if (row) {
        if (isCorrect) {
          row.setAttribute("data-status", "DANG_XU_LY");
          row.cells[6].innerHTML = `<span style="color: #3b82f6;">Đã xác minh (Đang xử lý)</span>`;
        } else {
          // --- ĐOẠN QUAN TRỌNG: CHUYỂN SANG BẢNG SPAM ---
          const spamTableBody = document.querySelector(
            "#spam-table-container tbody",
          );
          const newSpamRow = document.createElement("tr");

          // Lấy dữ liệu hiện tại từ dòng cũ để đưa sang bảng spam
          const imgUrl = row.cells[1].querySelector("img").src;
          const reporterName = row.cells[3].querySelector("div").innerText;
          const address = row.cells[4].innerText;
          const currentTime = new Date().toLocaleString("vi-VN");

          newSpamRow.innerHTML = `
                        <td>${id}</td>
                        <td>${reporterName}</td>
                        <td><img src="${imgUrl}" class="img-table" /></td>
                        <td>Báo cáo bị từ chối bởi Admin</td>
                        <td>${address}</td>
                        <td>${currentTime}</td>
                    `;
          spamTableBody.insertBefore(newSpamRow, spamTableBody.firstChild);

          // Xóa dòng cũ ở bảng chính vì nó đã là Spam
          row.remove();
        }
      }

      // 3. Cập nhật lại tất cả con số
      updateTabCounts();
      updateSliderVisibility();
    }
  } catch (error) {
    console.error(error);
  }
}
// Hàm bổ trợ để tính lại số lượng trên các Tab mà không cần load trang
function updateTabCounts() {
  // Đếm hàng ở bảng chính
  const rows = document.querySelectorAll("#table-body-main .incident-row");
  let pending = 0,
    processing = 0,
    done = 0;

  rows.forEach((row) => {
    const status = row.getAttribute("data-status");
    if (status === "CHO_XU_LY") pending++;
    else if (status === "DANG_XU_LY") processing++;
    else if (status === "HOAN_THANH") done++;
  });

  // Đếm hàng ở bảng Spam
  const spamRows = document.querySelectorAll(
    "#spam-table-container tbody tr",
  ).length;

  const tabCounts = document.querySelectorAll(".tab-count");
  if (tabCounts.length >= 5) {
    tabCounts[0].innerText = rows.length; // Tất cả
    tabCounts[1].innerText = pending; // Chờ
    tabCounts[2].innerText = processing; // Đang
    tabCounts[3].innerText = done; // Xong
    tabCounts[4].innerText = spamRows; // Spam (Tab cuối cùng)
  }
}
document.addEventListener("DOMContentLoaded", function () {
  document.querySelectorAll(".pending-item").forEach((item) => {
    const reportId = item.id.replace("pending-report-", "");
    loadNearbyCameras(reportId);
  });

  // Load cho các item trong bảng bên dưới
  document.querySelectorAll(".incident-row").forEach((row) => {
    const reportId = row.id.replace("report-", "");
    loadNearbyCameras(reportId);
  });

  // Highlight item nếu có query parameter id
  const urlParams = new URLSearchParams(window.location.search);
  const highlightId = urlParams.get("id");
  if (highlightId) {
    const targetCard = document.getElementById("report-" + highlightId);
    if (targetCard) {
      // Highlight card
      targetCard.style.backgroundColor = "#fef3c7";
      targetCard.style.border = "2px solid #f59e0b";
      targetCard.style.boxShadow = "0 0 20px rgba(245, 158, 11, 0.5)";

      // Scroll vào view
      setTimeout(() => {
        targetCard.scrollIntoView({
          behavior: "smooth",
          block: "center",
        });
      }, 100);

      // Bỏ highlight sau 3 giây
      setTimeout(() => {
        targetCard.style.backgroundColor = "";
        targetCard.style.border = "";
        targetCard.style.boxShadow = "";
      }, 3000);
    }
  }
  updateSliderVisibility();
});

async function loadNearbyCameras(reportId) {
  if (cameraLoaded.has(reportId)) {
    return;
  }
  cameraLoaded.add(reportId);
  // 1. Tìm tọa độ từ Card Duyệt nhanh hoặc Dòng trong bảng
  const sourceElement =
    document.getElementById("pending-report-" + reportId) ||
    document.getElementById("report-" + reportId);

  if (!sourceElement) return;

  const lat = sourceElement.getAttribute("data-lat");
  const lng = sourceElement.getAttribute("data-lng");

  if (!lat || !lng || lat === "0.0") return;

  try {
    const res = await fetch(
      `/admin/quan-ly-camera/near-by-incident/${reportId}`,
    );
    const cameras = await res.json();

    if (cameras && cameras.length > 0) {
      // --- CẬP NHẬT CHO CARD DUYỆT NHANH (Phần trên) ---
      // --- CẬP NHẬT CHO CARD DUYỆT NHANH (Phần trên) ---
      const pendingList = document.getElementById(
        "cam-list-pending-" + reportId,
      );
      if (pendingList) {
        pendingList.innerHTML = "";
        cameras.forEach((cam) => {
          const badge = document.createElement("div");
          // Style này giúp badge chứa cả icon và tên trên một hàng, trông chuyên nghiệp hơn
          badge.style = `
            background: #fee2e2; 
            padding: 2px 8px; 
            border-radius: 6px; 
            cursor: pointer; 
            border: 1px solid #fecaca; 
            font-size: 11px;
            display: flex;
            align-items: center;
            gap: 4px;
            color: #b91c1c;
            font-weight: 600;
        `;

          // Chèn cả Icon và Tên camera vào đây
          badge.innerHTML = `
            <i class="fa-solid fa-video" style="color: #ef4444;"></i>
            <span>${cam.tenCamera}</span>
        `;

          badge.title = "Bấm để xem trực tiếp " + cam.tenCamera;
          badge.onclick = (e) => {
            e.stopPropagation();
            cam.videoUrl
              ? window.open(cam.videoUrl, "_blank")
              : alert("Camera này không có luồng trực tiếp!");
          };
          pendingList.appendChild(badge);
        });
      }
      // --- CẬP NHẬT CHO BẢNG TẤT CẢ (Phần dưới - Cột Camera mới) ---
      const tableArea = document.getElementById("cam-area-" + reportId);
      const tableList = document.getElementById("cam-list-" + reportId);

      if (tableArea && tableList) {
        tableArea.style.display = "flex";
        tableList.innerHTML = "";
        cameras.forEach((cam) => {
          const camItem = document.createElement("div");
          // Style cho item camera trong bảng: có viền, icon và tên
          camItem.style = `
            background: #f1f5f9;
            border: 1px solid #cbd5e1;
            padding: 3px 8px;
            border-radius: 4px;
            font-size: 11px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 5px;
            white-space: nowrap;
        `;

          camItem.innerHTML = `
            <i class="fa-solid fa-camera" style="color: #475569;"></i>
            <span style="font-weight: 500;">${cam.tenCamera}</span>
        `;

          camItem.onclick = () =>
            cam.videoUrl
              ? window.open(cam.videoUrl, "_blank")
              : alert("Không có luồng!");

          tableList.appendChild(camItem);
        });
      }
    }
  } catch (e) {
    console.error("Lỗi quét camera cho ID: " + reportId, e);
  }
}
let currentPendingPage = 0;
const itemsPerPage = 3;

// Logic Chuyển trang cho Slider phía trên
function updateSliderVisibility() {
  const items = document.querySelectorAll(".pending-item");
  items.forEach((item, index) => {
    const start = currentPendingPage * itemsPerPage;
    const end = start + itemsPerPage;
    item.style.display = index >= start && index < end ? "flex" : "none";
  });
}

function nextPendingPage() {
  const totalItems = document.querySelectorAll(".pending-item").length;
  if ((currentPendingPage + 1) * itemsPerPage < totalItems) {
    currentPendingPage++;
    updateSliderVisibility();
  }
}

function prevPendingPage() {
  if (currentPendingPage > 0) {
    currentPendingPage--;
    updateSliderVisibility();
  }
}

// Logic Chuyển Tab bên dưới
window.switchTab = function (type) {
  // Cập nhật UI nút tab
  document
    .querySelectorAll(".tab-item")
    .forEach((tab) => tab.classList.remove("active"));
  event.currentTarget.classList.add("active");

  const mainContainer = document.getElementById("incident-table-container");
  const spamContainer = document.getElementById("spam-table-container");
  const rows = document.querySelectorAll(".incident-row");

  if (type === "spam") {
    mainContainer.style.display = "none";
    spamContainer.style.display = "block";
  } else {
    mainContainer.style.display = "block";
    spamContainer.style.display = "none";

    rows.forEach((row) => {
      const status = row.getAttribute("data-status");
      if (type === "all") row.style.display = "";
      else if (type === "pending" && status === "CHO_XU_LY")
        row.style.display = "";
      else if (type === "processing" && status === "DANG_XU_LY")
        row.style.display = "";
      else if (type === "done" && status === "HOAN_THANH")
        row.style.display = "";
      else row.style.display = "none";
    });
  }
};
