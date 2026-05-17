document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const formData = new FormData(e.target);
  const errorBox = document.getElementById("errorBox");

  try {
    const res = await fetch("/truso/login", {
      method: "POST",
      body: formData,
      credentials: "include",
    });

    const data = await res.json();

    if (res.ok) {
      console.log("Login OK:", data);

      // chuyển trang chủ
      window.location.href = "/truso/trang-chu";
    } else {
      errorBox.style.display = "block";
      errorBox.innerText = data.message || "Đăng nhập thất bại";
    }
  } catch (err) {
    errorBox.style.display = "block";
    errorBox.innerText = "Lỗi kết nối server";
  }
});
