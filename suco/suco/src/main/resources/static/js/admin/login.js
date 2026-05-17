document
  .getElementById("loginForm")
  .addEventListener("submit", async function (e) {
    e.preventDefault();

    const form = e.target;
    const errorBox = document.getElementById("error");

    errorBox.style.display = "none";

    try {
      const res = await fetch("/admin/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: form.email.value,
          password: form.password.value,
        }),
      });

      const contentType = res.headers.get("content-type") || "";
      const data = contentType.includes("application/json")
        ? await res.json()
        : await res.text();

      if (res.ok) {
        // lưu JWT
        localStorage.setItem("token", data.token);

        // chuyển thẳng sang dashboard admin
        window.location.href = "/admin/trang-chu";
      } else {
        errorBox.style.display = "block";
        errorBox.innerText =
          typeof data === "string"
            ? data
            : data?.message || "Đăng nhập thất bại";
      }
    } catch (err) {
      errorBox.style.display = "block";
      errorBox.innerText = "Lỗi kết nối server";
    }
  });
