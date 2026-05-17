const toggleBtn = document.querySelector(".toggle-btn");
const sidebar = document.getElementById("sidebar");

toggleBtn.addEventListener("click", () => {
  sidebar.classList.toggle("collapsed");
});

function deleteGoi(id) {
  if (!confirm("Xóa gói này?")) return;

  fetch(`/api/goi/delete/${id}`, {
    method: "DELETE",
  })
    .then((res) => res.text())
    .then((msg) => {
      alert(msg);
      location.reload();
    })
    .catch((err) => {
      alert("Lỗi khi xóa!");
      console.error(err);
    });
}

document.getElementById("formGoi").addEventListener("submit", function (e) {
  e.preventDefault();

  const data = {
    ten: document.querySelector("[name='ten']").value,
    thoiHan: document.querySelector("[name='thoiHan']").value,
    gia: document.querySelector("[name='gia']").value,
    khoangCachMienPhi: document.querySelector("[name='khoangCachMienPhi']")
      .value,
    uuDai: document.querySelector("[name='uuDai']").value,
  };

  fetch("/api/goi/create", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  })
    .then((res) => res.text())
    .then((msg) => {
      alert(msg);
      location.reload();
    });
});
