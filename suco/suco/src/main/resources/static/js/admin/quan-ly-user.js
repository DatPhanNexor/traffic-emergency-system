function confirmDeleteUser(btn) {
  const uid = btn.dataset.uid;

  if (confirm("Xóa người dùng này? Hành động không thể hoàn tác!")) {
    fetch(`/admin/quan-ly-user/delete/${uid}`, {
      method: "DELETE",
    })
      .then((response) => {
        if (response.ok) {
          alert("Xóa user thành công!");
          window.location.reload();
        } else if (response.status === 404) {
          alert("User không tồn tại!");
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
