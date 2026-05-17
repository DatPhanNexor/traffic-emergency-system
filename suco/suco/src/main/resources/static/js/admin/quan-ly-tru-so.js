function openModal() {
  document.getElementById("addModal").style.display = "flex";
}
function closeModal() {
  document.getElementById("addModal").style.display = "none";
}

function filterTable() {
  let input = document.getElementById("searchInput").value.toUpperCase();
  let rows = document.getElementById("stationTable").getElementsByTagName("tr");
  for (let i = 1; i < rows.length; i++) {
    let text = rows[i].textContent || rows[i].innerText;
    rows[i].style.display =
      text.toUpperCase().indexOf(input) > -1 ? "" : "none";
  }
}

// Đóng modal khi click ra ngoài
window.onclick = function (event) {
  if (event.target == document.getElementById("addModal")) closeModal();
};
function handleStationSubmit(event) {
  event.preventDefault();

  const form = document.getElementById("addStationForm");
  const formData = new FormData(form);

  fetch("/admin/quan-ly-tru-so/them", {
    method: "POST",
    body: formData,
  })
    .then((response) => {
      if (response.ok) {
        alert("Thêm trụ sở thành công!");
        closeModal();
        window.location.reload();
      } else {
        response.text().then((text) => alert("Lỗi: " + text));
      }
    })
    .catch((error) => console.error("Error:", error));
}
function confirmDeleteTruSo(id) {
  if (confirm("Bạn có chắc chắn muốn xóa trụ sở này?")) {
    fetch(`/admin/quan-ly-tru-so/delete/${id}`, {
      method: "DELETE",
    })
      .then((response) => {
        if (response.ok) {
          alert("Xóa trụ sở thành công!");
          window.location.reload();
        } else if (response.status === 404) {
          alert("Trụ sở không tồn tại!");
        } else {
          response.text().then((txt) => alert("Lỗi: " + txt));
        }
      })
      .catch((error) => {
        console.error("Error:", error);
        alert("Không thể kết nối server");
      });
  }
}
