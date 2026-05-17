function openModal() {
  document.getElementById("cameraModal").style.display = "flex";
}
function closeModal() {
  document.getElementById("cameraModal").style.display = "none";
}

function filterTable() {
  let input = document.getElementById("searchInput").value.toUpperCase();
  let rows = document
    .querySelector("#cameraTable tbody")
    .getElementsByTagName("tr");
  for (let row of rows) {
    let text = row.innerText.toUpperCase();
    row.style.display = text.includes(input) ? "" : "none";
  }
}

function handleFormSubmit(event) {
  event.preventDefault();

  const form = event.target;
  const formData = new FormData(form);

  fetch("/admin/quan-ly-camera/them", {
    method: "POST",
    body: formData,
  })
    .then((response) => {
      if (response.ok) {
        alert("Thêm camera thành công!");
        closeModal();
        window.location.reload();
      } else {
        response.text().then((txt) => {
          alert("Có lỗi xảy ra: " + txt);
        });
      }
    })
    .catch((error) => {
      console.error("Error:", error);
      alert("Lỗi kết nối đến máy chủ.");
    });
}

function openMapPicker() {
  // Mở trang chủ admin với map để chọn vị trí
  window.open(
    "/admin/trang-chu?pickLocation=true",
    "_blank",
    "width=1200,height=800",
  );

  // Lắng nghe message từ cửa sổ con
  window.addEventListener("message", function (event) {
    if (event.data.type === "locationPicked") {
      document.getElementById("kinhDo").value = event.data.lng;
      document.getElementById("viDo").value = event.data.lat;
    }
  });
}

function openCameraDetail(id) {
  // Fetch chi tiết camera từ server
  fetch(`/admin/quan-ly-camera/${id}/detail`)
    .then((response) => {
      if (!response.ok) {
        throw new Error("Không tìm thấy camera");
      }
      return response.json();
    })
    .then((data) => {
      displayCameraDetail(data);
    })
    .catch((error) => {
      console.error("Error:", error);
      alert("Không thể tải thông tin camera");
    });
}

function displayCameraDetail(data) {
  const modal = document.getElementById("cameraDetailModal");
  const videoContainer = document.getElementById("cameraDetailVideoContainer");
  const imageContainer = document.getElementById("cameraDetailImageContainer");
  const videoElement = document.getElementById("cameraDetailVideo");
  const videoSource = document.getElementById("videoSource");
  const imageElement = document.getElementById("cameraDetailImage");

  // Điền thông tin
  document.getElementById("detailCameraName").textContent =
    data.tenCamera || "Camera";
  document.getElementById("detailName").textContent = data.tenCamera || "-";
  document.getElementById("detailMoTa").textContent =
    data.moTa || "Chưa có miêu tả";

  // Xử lý tọa độ - kiểm tra null và 0
  const kinhDo = data.kinhDo != null && data.kinhDo != 0 ? data.kinhDo : null;
  const viDo = data.viDo != null && data.viDo != 0 ? data.viDo : null;
  document.getElementById("detailKinhDo").textContent =
    kinhDo != null ? kinhDo : "Chưa có";
  document.getElementById("detailViDo").textContent =
    viDo != null ? viDo : "Chưa có";

  // Xử lý video
  if (
    data.videoUrl &&
    data.videoUrl.trim() !== "" &&
    data.videoUrl !== "null"
  ) {
    videoSource.src = data.videoUrl;
    videoElement.load();
    videoContainer.style.display = "block";
    imageContainer.style.display = "none";
  } else if (
    data.anhCamera &&
    data.anhCamera.trim() !== "" &&
    data.anhCamera !== "null"
  ) {
    // Nếu không có video thì hiển thị ảnh
    imageElement.src = data.anhCamera;
    imageContainer.style.display = "block";
    videoContainer.style.display = "none";
  } else {
    videoContainer.style.display = "none";
    imageContainer.style.display = "none";
  }

  // Link Google Maps
  const mapLink = document.getElementById("detailMapLink");
  if (kinhDo != null && viDo != null) {
    mapLink.href = `https://www.google.com/maps?q=${viDo},${kinhDo}`;
    mapLink.style.display = "inline";
  } else {
    mapLink.style.display = "none";
  }

  modal.style.display = "flex";
}

//
function closeCameraDetail() {
  const modal = document.getElementById("cameraDetailModal");
  const videoElement = document.getElementById("cameraDetailVideo");
  videoElement.pause();
  videoElement.currentTime = 0;
  modal.style.display = "none";
}

// Xử lý click ngoài modal để đóng
window.onclick = (e) => {
  const cameraModal = document.getElementById("cameraModal");
  const cameraDetailModal = document.getElementById("cameraDetailModal");
  if (e.target == cameraModal) closeModal();
  if (e.target == cameraDetailModal) closeCameraDetail();
};
function confirmDelete(id) {
  if (confirm("Bạn có chắc chắn muốn xóa camera này?")) {
    // Gửi yêu cầu POST đến server
    fetch(`/admin/quan-ly-camera/${id}`, {
      method: "DELETE",
    })
      .then((response) => {
        if (response.ok) {
          alert("Xóa camera thành công!");
          window.location.reload(); // Load lại trang để cập nhật danh sách
        } else {
          response.text().then((txt) => alert("Lỗi: " + txt));
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert("Không thể kết nối đến máy chủ");
      });
  }
}
