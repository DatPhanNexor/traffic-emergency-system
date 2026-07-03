const JSON_HEADERS = {
  "Content-Type": "application/json"
};

async function parseBody(response) {
  const text = await response.text();

  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch (error) {
    return text;
  }
}

function authHeader(token) {
  if (!token) {
    return {};
  }

  return {
    Authorization: `Bearer ${token}`
  };
}

function cookieHeader(cookie) {
  if (!cookie) {
    return {};
  }

  return {
    Cookie: cookie
  };
}

async function requestJson(url, options = {}) {
  const {
    method = "GET",
    token,
    cookie,
    body
  } = options;

  const headers = {
    ...(body !== undefined ? JSON_HEADERS : {}),
    ...authHeader(token),
    ...cookieHeader(cookie)
  };

  const response = await fetch(url, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined
  });

  const data = await parseBody(response);

  if (!response.ok) {
    const message =
      data && data.message
        ? data.message
        : typeof data === "string"
          ? data
          : `HTTP ${response.status}`;

    const error = new Error(message);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

const SVP03Api = {
  // Feature 10 - Quản lý Gói cứu hộ
  getDanhSachGoi() {
    return requestJson("/api/goi/danh-sach");
  },

  createGoi(payload, adminToken) {
    return requestJson("/api/goi/create", {
      method: "POST",
      token: adminToken,
      body: payload
    });
  },

  updateGoi(id, payload, adminToken) {
    return requestJson(`/api/goi/update/${id}`, {
      method: "PATCH",
      token: adminToken,
      body: payload
    });
  },

  deleteGoi(id, adminToken) {
    return requestJson(`/api/goi/delete/${id}`, {
      method: "DELETE",
      token: adminToken
    });
  },

  // Feature 11 - Quản lý Gói sở hữu
  getGoiDeMua() {
    return requestJson("/api/mua-goi/danh-sach");
  },

  dangKyGoi(goiId, userToken) {
    return requestJson("/api/mua-goi/dang-ky", {
      method: "POST",
      token: userToken,
      body: {
        goiId
      }
    });
  },

  getMyPackages(userToken) {
    return requestJson("/api/mua-goi/my-packages", {
      token: userToken
    });
  },

  cancelGoi(muaGoiId, userToken) {
    return requestJson(`/api/mua-goi/cancel/${muaGoiId}`, {
      method: "POST",
      token: userToken
    });
  },

  // Feature 12 - User ra tín hiệu cứu hộ
  submitSOS(payload, userToken) {
    return requestJson("/api/tin-hieu-sos/submit", {
      method: "POST",
      token: userToken,
      body: payload
    });
  },

  cancelSOS(sosId, userToken) {
    return requestJson(`/api/tin-hieu-sos/cancel/${sosId}`, {
      method: "POST",
      token: userToken
    });
  },

  xacNhanThanhToan(hoaDonId, userToken, quaId) {
    const query = quaId ? `?quaId=${encodeURIComponent(quaId)}` : "";

    return requestJson(`/api/hoa-don/xac-nhan/${hoaDonId}${query}`, {
      method: "POST",
      token: userToken
    });
  },

  // Feature 13 - Trụ sở xử lý tín hiệu
  updateSosStatus(sosId, status, trusoCookie) {
    return requestJson(
      `/api/tin-hieu-sos/cap-nhat-trang-thai/${sosId}?status=${encodeURIComponent(status)}`,
      {
        method: "POST",
        cookie: trusoCookie
      }
    );
  },

  taoHoaDon(payload, trusoCookie) {
    return requestJson("/api/hoa-don/tao", {
      method: "POST",
      cookie: trusoCookie,
      body: payload
    });
  },

  getSosHistory(trusoCookie) {
    return requestJson("/api/tin-hieu-sos/history", {
      cookie: trusoCookie
    });
  }
};

if (typeof module !== "undefined" && module.exports) {
  module.exports = {
    parseBody,
    authHeader,
    cookieHeader,
    requestJson,
    SVP03Api
  };
}