// Toggle Sidebar Logic
const toggleBtn = document.querySelector(".toggle-btn");
const sidebar = document.getElementById("sidebar");

toggleBtn.addEventListener("click", () => {
  sidebar.classList.toggle("collapsed");
});
