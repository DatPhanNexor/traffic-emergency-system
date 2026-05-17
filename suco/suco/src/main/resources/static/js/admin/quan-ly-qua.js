function toggleEditVoucherFields() {
  const loai = document.getElementById("editLoai").value;
  const box = document.getElementById("editVoucherFields");

  if (loai === "VOUCHER") {
    box.style.display = "block";
  } else {
    box.style.display = "none";

    // reset
    document.getElementById("editGiam").value = "";
    document.getElementById("editToiDa").value = "";
  }
}

document.addEventListener("DOMContentLoaded", function () {
  // Toggle Sidebar
  const toggleBtn = document.querySelector(".toggle-btn");
  const sidebar = document.getElementById("sidebar");

  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed");
    });
  }
  document
    .getElementById("formAddQua")
    .addEventListener("submit", function (e) {
      e.preventDefault();

      const formData = new FormData(this);

      fetch("/admin/quan-ly-qua/add", {
        method: "POST",
        body: formData,
      })
        .then((res) => res.json())
        .then((data) => {
          alert(data.message);
          location.reload();
        })
        .catch(() => alert("Lỗi server"));
    });

  // Voucher logic
  const loaiSelect = document.getElementById("selectLoaiQua");
  const voucherFields = document.getElementById("voucherFields");

  function updateFields() {
    if (!loaiSelect || !voucherFields) return;

    if (loaiSelect.value === "VOUCHER") {
      voucherFields.style.display = "block";
    } else {
      voucherFields.style.display = "none";

      // reset field
      const percent = document.querySelector('[name="giaTriGiamPercent"]');
      const max = document.querySelector('[name="giaTriToiDa"]');

      if (percent) percent.value = "";
      if (max) max.value = "";
    }
  }

  if (loaiSelect) {
    loaiSelect.addEventListener("change", updateFields);
    updateFields(); // chạy ngay khi load
  }

  // Xóa quà
  window.deleteQua = function (id) {
    if (!confirm("Xóa mục này?")) return;

    fetch(`/admin/quan-ly-qua/delete/${id}`, {
      method: "DELETE",
    })
      .then((res) => {
        if (res.ok) {
          const row = document
            .querySelector(`button[onclick="deleteQua(${id})"]`)
            .closest("tr");
          row.remove();
        } else {
          alert("Xóa thất bại");
        }
      })
      .catch(() => alert("Lỗi server"));
  };
});

// Đổ dữ liệu hiện tại vào modal
function openEditModal(btn) {
  const q = btn.dataset;

  document.getElementById("editId").value = q.id;
  document.getElementById("editTen").value = q.ten;
  document.getElementById("editMoTa").value = q.mota;
  document.getElementById("editLoai").value = q.loai;
  document.getElementById("editDiem").value = q.diem;

  document.getElementById("editGiam").value = q.giam || "";
  document.getElementById("editToiDa").value = q.toida || "";
  document.getElementById("editNgayKetThuc").value = q.ngayketthuc || "";

  document.getElementById("previewHinhAnh").src = q.hinhanh
    ? "/uploads/" + q.hinhanh
    : "/uploads/default.png";

  toggleEditVoucherFields();

  document.getElementById("editModal").style.display = "block";
}

function closeModal() {
  document.getElementById("editModal").style.display = "none";
}
function submitEdit() {
  const id = document.getElementById("editId").value;

  const formData = new FormData();
  formData.append("ten", document.getElementById("editTen").value);
  formData.append("moTa", document.getElementById("editMoTa").value);
  formData.append("loai", document.getElementById("editLoai").value);
  formData.append("diem", document.getElementById("editDiem").value);
  formData.append(
    "giaTriGiamPercent",
    document.getElementById("editGiam").value || "",
  );
  formData.append(
    "giaTriToiDa",
    document.getElementById("editToiDa").value || "",
  );
  formData.append(
    "ngayKetThuc",
    document.getElementById("editNgayKetThuc").value || "",
  );

  // 👇 QUAN TRỌNG NHẤT
  const file = document.getElementById("editHinhAnh").files[0];
  if (file) {
    formData.append("hinhAnh", file);
  }

  fetch(`/admin/quan-ly-qua/edit/${id}`, {
    method: "PATCH",
    body: formData,
  }).then((res) => {
    if (res.ok) {
      alert("Cập nhật thành công");
      location.reload();
    } else {
      alert("Lỗi update");
    }
  });
}
function changeStatus(select) {
  const id = select.dataset.id;
  const trangThai = select.value;

  const confirmChange = confirm("Bạn có chắc muốn thay đổi trạng thái không?");

  if (!confirmChange) {
    // Hủy → reset lại giá trị cũ (tránh UI bị đổi)
    location.reload(); // hoặc giữ nguyên trạng thái cũ
    return;
  }

  fetch(`/admin/quan-ly-qua/edit/${id}`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      trangThai: trangThai,
    }),
  })
    .then((res) => {
      if (!res.ok) throw new Error();

      alert("Cập nhật trạng thái thành công ✅");
      location.reload(); // OK → reload lại trang
    })
    .catch(() => {
      alert("Lỗi cập nhật trạng thái ❌");
      location.reload(); // lỗi cũng reload để về trạng thái cũ
    });
}
