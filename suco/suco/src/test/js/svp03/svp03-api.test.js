const {
  parseBody,
  authHeader,
  cookieHeader,
  requestJson,
  SVP03Api
} = require("../../../main/resources/static/js/svp03/svp03-api");

function mockFetchResponse(status, body) {
  global.fetch.mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    text: jest.fn().mockResolvedValue(body)
  });
}

describe("SVP-03 White-box Jest Coverage - helper functions", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  test("WBT-SVP03-00.1 parseBody phủ nhánh body rỗng", async () => {
    const response = {
      text: jest.fn().mockResolvedValue("")
    };

    await expect(parseBody(response)).resolves.toBeNull();
  });

  test("WBT-SVP03-00.2 parseBody phủ nhánh JSON hợp lệ", async () => {
    const response = {
      text: jest.fn().mockResolvedValue("{\"status\":\"success\"}")
    };

    await expect(parseBody(response)).resolves.toEqual({
      status: "success"
    });
  });

  test("WBT-SVP03-00.3 parseBody phủ nhánh không phải JSON", async () => {
    const response = {
      text: jest.fn().mockResolvedValue("Xóa thành công")
    };

    await expect(parseBody(response)).resolves.toBe("Xóa thành công");
  });

  test("WBT-SVP03-00.4 authHeader phủ nhánh có token và không token", () => {
    expect(authHeader()).toEqual({});
    expect(authHeader("dev-token")).toEqual({
      Authorization: "Bearer dev-token"
    });
  });

  test("WBT-SVP03-00.5 cookieHeader phủ nhánh có cookie và không cookie", () => {
    expect(cookieHeader()).toEqual({});
    expect(cookieHeader("JSESSIONID=abc")).toEqual({
      Cookie: "JSESSIONID=abc"
    });
  });

  test("WBT-SVP03-00.6 requestJson success có body, token, cookie", async () => {
    mockFetchResponse(200, "{\"ok\":true}");

    const result = await requestJson("/test", {
      method: "POST",
      token: "dev-token",
      cookie: "JSESSIONID=abc",
      body: {
        goiId: 1
      }
    });

    expect(result).toEqual({
      ok: true
    });

    expect(global.fetch).toHaveBeenCalledWith("/test", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer dev-token",
        Cookie: "JSESSIONID=abc"
      },
      body: JSON.stringify({
        goiId: 1
      })
    });
  });

  test("WBT-SVP03-00.7 requestJson error JSON có message", async () => {
    mockFetchResponse(400, "{\"message\":\"goiId không được để trống\"}");

    await expect(requestJson("/test-error")).rejects.toMatchObject({
      status: 400,
      message: "goiId không được để trống"
    });
  });

  test("WBT-SVP03-00.8 requestJson error body text", async () => {
    mockFetchResponse(404, "Gói không tồn tại");

    await expect(requestJson("/test-not-found")).rejects.toMatchObject({
      status: 404,
      message: "Gói không tồn tại"
    });
  });

  test("WBT-SVP03-00.9 requestJson error body rỗng", async () => {
    mockFetchResponse(500, "");

    await expect(requestJson("/test-empty-error")).rejects.toMatchObject({
      status: 500,
      message: "HTTP 500"
    });
  });
});

describe("Feature 10 - Quản lý Gói cứu hộ", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  test("WBT-F10-01 getDanhSachGoi gọi đúng endpoint", async () => {
    mockFetchResponse(200, "[{\"id\":1,\"ten\":\"Gói cơ bản\"}]");

    const result = await SVP03Api.getDanhSachGoi();

    expect(result[0].id).toBe(1);
    expect(global.fetch).toHaveBeenCalledWith("/api/goi/danh-sach", {
      method: "GET",
      headers: {},
      body: undefined
    });
  });

  test("WBT-F10-02 createGoi tạo gói thành công", async () => {
    mockFetchResponse(200, "{\"id\":10,\"ten\":\"Gói SVP03\"}");

    const payload = {
      ten: "Gói SVP03",
      gia: 100000,
      thoiHan: 30,
      khoangCachMienPhi: 20,
      uuDai: "Hỗ trợ nhanh"
    };

    const result = await SVP03Api.createGoi(payload, "admin-token");

    expect(result.id).toBe(10);
    expect(global.fetch).toHaveBeenCalledWith("/api/goi/create", expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({
        "Content-Type": "application/json",
        Authorization: "Bearer admin-token"
      }),
      body: JSON.stringify(payload)
    }));
  });

  test("WBT-F10-03 createGoi thất bại khi tên rỗng", async () => {
    mockFetchResponse(400, "{\"message\":\"Tên gói không được để trống\"}");

    await expect(SVP03Api.createGoi({
      ten: ""
    }, "admin-token")).rejects.toMatchObject({
      status: 400,
      message: "Tên gói không được để trống"
    });
  });

  test("WBT-F10-04 updateGoi gọi PATCH đúng endpoint", async () => {
    mockFetchResponse(200, "{\"id\":1,\"ten\":\"Gói updated\"}");

    const payload = {
      ten: "Gói updated",
      gia: 120000
    };

    const result = await SVP03Api.updateGoi(1, payload, "admin-token");

    expect(result.ten).toBe("Gói updated");
    expect(global.fetch).toHaveBeenCalledWith("/api/goi/update/1", expect.objectContaining({
      method: "PATCH",
      body: JSON.stringify(payload)
    }));
  });

  test("WBT-F10-05 deleteGoi thành công", async () => {
    mockFetchResponse(200, "Xóa thành công");

    const result = await SVP03Api.deleteGoi(1, "admin-token");

    expect(result).toBe("Xóa thành công");
    expect(global.fetch).toHaveBeenCalledWith("/api/goi/delete/1", expect.objectContaining({
      method: "DELETE"
    }));
  });

  test("WBT-F10-06 deleteGoi ID không tồn tại", async () => {
    mockFetchResponse(404, "Gói không tồn tại");

    await expect(SVP03Api.deleteGoi(999999999, "admin-token")).rejects.toMatchObject({
      status: 404,
      message: "Gói không tồn tại"
    });
  });
});

describe("Feature 11 - Quản lý Gói sở hữu", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  test("WBT-F11-01 getGoiDeMua lấy danh sách gói để mua", async () => {
    mockFetchResponse(200, "[{\"id\":1,\"ten\":\"Gói cơ bản\"}]");

    const result = await SVP03Api.getGoiDeMua();

    expect(Array.isArray(result)).toBe(true);
    expect(global.fetch).toHaveBeenCalledWith("/api/mua-goi/danh-sach", expect.objectContaining({
      method: "GET"
    }));
  });

  test("WBT-F11-02 dangKyGoi thành công", async () => {
    mockFetchResponse(200, "{\"status\":\"success\",\"message\":\"Đăng ký gói thành công\"}");

    const result = await SVP03Api.dangKyGoi(1, "dev-token");

    expect(result.status).toBe("success");
    expect(global.fetch).toHaveBeenCalledWith("/api/mua-goi/dang-ky", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({
        goiId: 1
      })
    }));
  });

  test("WBT-F11-03 dangKyGoi thiếu token bị lỗi", async () => {
    mockFetchResponse(401, "{\"message\":\"Xác thực thất bại\"}");

    await expect(SVP03Api.dangKyGoi(1)).rejects.toMatchObject({
      status: 401
    });
  });

  test("WBT-F11-04 dangKyGoi goiId không tồn tại", async () => {
    mockFetchResponse(404, "{\"message\":\"Không tìm thấy gói\"}");

    await expect(SVP03Api.dangKyGoi(999999999, "dev-token")).rejects.toMatchObject({
      status: 404,
      message: "Không tìm thấy gói"
    });
  });

  test("WBT-F11-05 getMyPackages thành công", async () => {
    mockFetchResponse(200, "[{\"id\":5,\"trangThai\":\"ACTIVE\"}]");

    const result = await SVP03Api.getMyPackages("dev-token");

    expect(result[0].trangThai).toBe("ACTIVE");
    expect(global.fetch).toHaveBeenCalledWith("/api/mua-goi/my-packages", expect.objectContaining({
      method: "GET"
    }));
  });

  test("WBT-F11-06 cancelGoi thành công", async () => {
    mockFetchResponse(200, "{\"message\":\"Đã hủy gói thành công\"}");

    const result = await SVP03Api.cancelGoi(5, "dev-token");

    expect(result.message).toBe("Đã hủy gói thành công");
    expect(global.fetch).toHaveBeenCalledWith("/api/mua-goi/cancel/5", expect.objectContaining({
      method: "POST"
    }));
  });
});

describe("Feature 12 - User ra tín hiệu cứu hộ", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  test("WBT-F12-01 submitSOS thành công", async () => {
    mockFetchResponse(200, "{\"id\":20,\"trangThai\":\"CHO_XU_LY\"}");

    const payload = {
      viDo: 10.762792,
      kinhDo: 106.660172,
      ghiChu: "Cần cứu hộ",
      hinhAnhBase64: null,
      ghiAmBase64: null
    };

    const result = await SVP03Api.submitSOS(payload, "dev-token");

    expect(result.id).toBe(20);
    expect(global.fetch).toHaveBeenCalledWith("/api/tin-hieu-sos/submit", expect.objectContaining({
      method: "POST",
      body: JSON.stringify(payload)
    }));
  });

  test("WBT-F12-02 submitSOS thiếu token", async () => {
    mockFetchResponse(401, "Xác thực thất bại");

    await expect(SVP03Api.submitSOS({
      viDo: 10.762792,
      kinhDo: 106.660172
    })).rejects.toMatchObject({
      status: 401
    });
  });

  test("WBT-F12-03 cancelSOS thành công", async () => {
    mockFetchResponse(200, "{\"message\":\"Hủy yêu cầu SOS thành công\"}");

    const result = await SVP03Api.cancelSOS(20, "dev-token");

    expect(result.message).toBe("Hủy yêu cầu SOS thành công");
    expect(global.fetch).toHaveBeenCalledWith("/api/tin-hieu-sos/cancel/20", expect.objectContaining({
      method: "POST"
    }));
  });

  test("WBT-F12-04 cancelSOS ID không tồn tại", async () => {
    mockFetchResponse(404, "");

    await expect(SVP03Api.cancelSOS(999999, "dev-token")).rejects.toMatchObject({
      status: 404
    });
  });

  test("WBT-F12-05 xacNhanThanhToan thành công không dùng voucher", async () => {
    mockFetchResponse(200, "{\"id\":100,\"trangThai\":\"PAID\"}");

    const result = await SVP03Api.xacNhanThanhToan(100, "dev-token");

    expect(result.trangThai).toBe("PAID");
    expect(global.fetch).toHaveBeenCalledWith("/api/hoa-don/xac-nhan/100", expect.objectContaining({
      method: "POST"
    }));
  });

  test("WBT-F12-06 xacNhanThanhToan có quaId", async () => {
    mockFetchResponse(200, "{\"id\":100,\"trangThai\":\"PAID\"}");

    await SVP03Api.xacNhanThanhToan(100, "dev-token", 3);

    expect(global.fetch).toHaveBeenCalledWith("/api/hoa-don/xac-nhan/100?quaId=3", expect.objectContaining({
      method: "POST"
    }));
  });

  test("WBT-F12-07 xacNhanThanhToan hóa đơn không thuộc user", async () => {
    mockFetchResponse(403, "{\"message\":\"Bạn không có quyền thanh toán hóa đơn này\"}");

    await expect(SVP03Api.xacNhanThanhToan(999, "dev-token")).rejects.toMatchObject({
      status: 403
    });
  });
});

describe("Feature 13 - Trụ sở xử lý tín hiệu", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  test("WBT-F13-01 updateSosStatus tiếp nhận SOS DANG_XU_LY", async () => {
    mockFetchResponse(200, "{\"status\":\"DANG_XU_LY\",\"message\":\"Cập nhật thành công\"}");

    const result = await SVP03Api.updateSosStatus(20, "DANG_XU_LY", "JSESSIONID=truso-session");

    expect(result.status).toBe("DANG_XU_LY");
    expect(global.fetch).toHaveBeenCalledWith(
      "/api/tin-hieu-sos/cap-nhat-trang-thai/20?status=DANG_XU_LY",
      expect.objectContaining({
        method: "POST",
        headers: {
          Cookie: "JSESSIONID=truso-session"
        }
      })
    );
  });

  test("WBT-F13-02 updateSosStatus hoàn thành SOS HOAN_THANH", async () => {
    mockFetchResponse(200, "{\"status\":\"HOAN_THANH\",\"message\":\"Cập nhật thành công\"}");

    const result = await SVP03Api.updateSosStatus(20, "HOAN_THANH", "JSESSIONID=truso-session");

    expect(result.status).toBe("HOAN_THANH");
  });

  test("WBT-F13-03 không cho hoàn thành SOS khi chưa tiếp nhận", async () => {
    mockFetchResponse(400, "{\"error\":\"Chỉ được hoàn thành SOS sau khi trụ sở đã tiếp nhận và đang xử lý\"}");

    await expect(
      SVP03Api.updateSosStatus(21, "HOAN_THANH", "JSESSIONID=truso-session")
    ).rejects.toMatchObject({
      status: 400
    });
  });

  test("WBT-F13-04 trụ sở khác không được cập nhật SOS", async () => {
    mockFetchResponse(403, "{\"error\":\"Trụ sở khác không được cập nhật SOS không thuộc quyền\"}");

    await expect(
      SVP03Api.updateSosStatus(20, "DANG_XU_LY", "JSESSIONID=other-truso")
    ).rejects.toMatchObject({
      status: 403
    });
  });

  test("WBT-F13-05 taoHoaDon thành công", async () => {
    mockFetchResponse(200, "{\"id\":1,\"thanhTien\":100000,\"tongThanhToan\":100000,\"trangThai\":\"PENDING\"}");

    const payload = {
      sosId: 20,
      tenSos: "Cứu hộ SOS test",
      xuLy: "Xử lý cứu hộ test",
      giaThuCong: 100000,
      quaId: null
    };

    const result = await SVP03Api.taoHoaDon(payload, "JSESSIONID=truso-session");

    expect(result.trangThai).toBe("PENDING");
    expect(global.fetch).toHaveBeenCalledWith("/api/hoa-don/tao", expect.objectContaining({
      method: "POST",
      body: JSON.stringify(payload)
    }));
  });

  test("WBT-F13-06 taoHoaDon có voucher quaId", async () => {
    mockFetchResponse(200, "{\"id\":1,\"soTienGiam\":50000,\"tongThanhToan\":50000}");

    const payload = {
      sosId: 20,
      tenSos: "Cứu hộ SOS test",
      xuLy: "Xử lý cứu hộ test",
      giaThuCong: 100000,
      quaId: 3
    };

    const result = await SVP03Api.taoHoaDon(payload, "JSESSIONID=truso-session");

    expect(result.soTienGiam).toBe(50000);
  });

  test("WBT-F13-07 không cho tạo hóa đơn trùng", async () => {
    mockFetchResponse(400, "{\"message\":\"Tín hiệu SOS này đã được tạo hóa đơn trước đó\"}");

    await expect(SVP03Api.taoHoaDon({
      sosId: 20,
      giaThuCong: 100000
    }, "JSESSIONID=truso-session")).rejects.toMatchObject({
      status: 400
    });
  });

  test("WBT-F13-08 getSosHistory thành công", async () => {
    mockFetchResponse(200, "[{\"id\":20,\"trangThai\":\"HOAN_THANH\"}]");

    const result = await SVP03Api.getSosHistory("JSESSIONID=truso-session");

    expect(result[0].trangThai).toBe("HOAN_THANH");
    expect(global.fetch).toHaveBeenCalledWith("/api/tin-hieu-sos/history", expect.objectContaining({
      method: "GET"
    }));
  });

  test("WBT-F13-09 getSosHistory không đăng nhập", async () => {
    mockFetchResponse(401, "Bạn chưa đăng nhập trụ sở");

    await expect(SVP03Api.getSosHistory()).rejects.toMatchObject({
      status: 401
    });
  });
});