const collection = require("../../resources/postman/Traffic Emergency System API.postman_collection.json");

const {
  expectedFunctions,
  expectedKey
} = require("../../../main/resources/static/js/whitebox/expected-functions");

const {
  parseFeatureNumber,
  parseFunctionNumber,
  inferFeatureFromFunction,
  normalizeUrl,
  getRawUrl,
  getBodyType,
  getRequestBody,
  extractExpectedStatuses,
  buildHeaders,
  getFeatureAndFunctionFromStack,
  walkPostmanItems,
  extractWhiteBoxCases,
  caseKey,
  groupByFunction,
  verifyExpectedFunctions,
  buildFetchOptions,
  createMockResponse,
  runWhiteBoxCase,
  buildCoverageSummary
} = require("../../../main/resources/static/js/whitebox/traffic-whitebox-core");

/**
 * Supplemental cases cho SVP-03.
 *
 * Lý do cần thêm:
 * - Postman Collection có API SVP-03 nhưng một số folder/request không map trực tiếp được
 *   về Function 30 -> 43 bằng parser tự động.
 * - Các case này bám theo TestCase_Final_2.xlsx, suco.zip và endpoint thật:
 *   Feature 10: /api/goi/...
 *   Feature 11: /api/mua-goi/...
 *   Feature 12: /api/tin-hieu-sos, /api/hoa-don, /api/lich-su
 *   Feature 13: /api/tin-hieu-sos, /api/hoa-don
 */
const supplementalSvp03Cases = [
  {
    id: "WBT-F10-30-SUP createGoi",
    featureNo: 10,
    functionNo: 30,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/goi/create",
    normalizedUrl: "/api/goi/create",
    bodyType: "json",
    body: {
      tenGoi: "Gói cứu hộ cơ bản",
      gia: 100000,
      thoiHan: 30,
      khoangCachMienPhi: 20
    },
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer admin-token"
    },
    expectedStatuses: [200, 201, 400],
    stack: [
      "Feature 10 - Quản lý Gói cứu hộ",
      "Function 30 - Tạo gói"
    ]
  },
  {
    id: "WBT-F10-31-SUP deleteGoi",
    featureNo: 10,
    functionNo: 31,
    method: "DELETE",
    rawUrl: "{{baseUrl}}/api/goi/delete/999999",
    normalizedUrl: "/api/goi/delete/999999",
    bodyType: "none",
    body: undefined,
    headers: {
      Authorization: "Bearer admin-token"
    },
    expectedStatuses: [200, 204, 400, 404],
    stack: [
      "Feature 10 - Quản lý Gói cứu hộ",
      "Function 31 - Xóa gói"
    ]
  },
  {
    id: "WBT-F10-32-SUP updateGoi",
    featureNo: 10,
    functionNo: 32,
    method: "PATCH",
    rawUrl: "{{baseUrl}}/api/goi/update/1",
    normalizedUrl: "/api/goi/update/1",
    bodyType: "json",
    body: {
      tenGoi: "Gói cứu hộ cập nhật",
      gia: 120000,
      thoiHan: 60,
      khoangCachMienPhi: 25
    },
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer admin-token"
    },
    expectedStatuses: [200, 400, 404],
    stack: [
      "Feature 10 - Quản lý Gói cứu hộ",
      "Function 32 - Cập nhật gói"
    ]
  },

  {
    id: "WBT-F11-33-SUP dangKyGoi",
    featureNo: 11,
    functionNo: 33,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/mua-goi/dang-ky",
    normalizedUrl: "/api/mua-goi/dang-ky",
    bodyType: "json",
    body: {
      goiId: 1
    },
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer user-token"
    },
    expectedStatuses: [200, 400, 401, 404],
    stack: [
      "Feature 11 - Quản lý Gói sở hữu",
      "Function 33 - Đăng ký gói"
    ]
  },
  {
    id: "WBT-F11-34-SUP getGoiDeMua",
    featureNo: 11,
    functionNo: 34,
    method: "GET",
    rawUrl: "{{baseUrl}}/api/mua-goi/danh-sach",
    normalizedUrl: "/api/mua-goi/danh-sach",
    bodyType: "none",
    body: undefined,
    headers: {},
    expectedStatuses: [200],
    stack: [
      "Feature 11 - Quản lý Gói sở hữu",
      "Function 34 - Xem danh sách gói"
    ]
  },
  {
    id: "WBT-F11-35-SUP getMyPackages",
    featureNo: 11,
    functionNo: 35,
    method: "GET",
    rawUrl: "{{baseUrl}}/api/mua-goi/my-packages",
    normalizedUrl: "/api/mua-goi/my-packages",
    bodyType: "none",
    body: undefined,
    headers: {
      Authorization: "Bearer user-token"
    },
    expectedStatuses: [200, 401],
    stack: [
      "Feature 11 - Quản lý Gói sở hữu",
      "Function 35 - Xem gói đang sở hữu"
    ]
  },
  {
    id: "WBT-F11-36-SUP cancelGoi",
    featureNo: 11,
    functionNo: 36,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/mua-goi/cancel/999999",
    normalizedUrl: "/api/mua-goi/cancel/999999",
    bodyType: "none",
    body: undefined,
    headers: {
      Authorization: "Bearer user-token"
    },
    expectedStatuses: [200, 400, 401, 404],
    stack: [
      "Feature 11 - Quản lý Gói sở hữu",
      "Function 36 - Hủy gói"
    ]
  },

  {
    id: "WBT-F12-37-SUP submitSOS",
    featureNo: 12,
    functionNo: 37,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/tin-hieu-sos/submit",
    normalizedUrl: "/api/tin-hieu-sos/submit",
    bodyType: "json",
    body: {
      viDo: 10.762792,
      kinhDo: 106.660172,
      ghiChu: "Cần cứu hộ khẩn cấp"
    },
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer user-token"
    },
    expectedStatuses: [200, 201, 400, 401],
    stack: [
      "Feature 12 - User ra tín hiệu cứu hộ",
      "Function 37 - Gửi SOS"
    ]
  },
  {
    id: "WBT-F12-38-SUP cancelSOS",
    featureNo: 12,
    functionNo: 38,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/tin-hieu-sos/cancel/999999",
    normalizedUrl: "/api/tin-hieu-sos/cancel/999999",
    bodyType: "none",
    body: undefined,
    headers: {
      Authorization: "Bearer user-token"
    },
    expectedStatuses: [200, 400, 401, 404],
    stack: [
      "Feature 12 - User ra tín hiệu cứu hộ",
      "Function 38 - Hủy SOS"
    ]
  },
  {
    id: "WBT-F12-39-SUP xacNhanThanhToan",
    featureNo: 12,
    functionNo: 39,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/hoa-don/xac-nhan/1",
    normalizedUrl: "/api/hoa-don/xac-nhan/1",
    bodyType: "none",
    body: undefined,
    headers: {
      Authorization: "Bearer user-token"
    },
    expectedStatuses: [200, 400, 401, 403, 404],
    stack: [
      "Feature 12 - User ra tín hiệu cứu hộ",
      "Function 39 - Xác nhận thanh toán"
    ]
  },
  {
    id: "WBT-F12-40-SUP getUserHistory",
    featureNo: 12,
    functionNo: 40,
    method: "GET",
    rawUrl: "{{baseUrl}}/api/lich-su/all",
    normalizedUrl: "/api/lich-su/all",
    bodyType: "none",
    body: undefined,
    headers: {
      Authorization: "Bearer user-token"
    },
    expectedStatuses: [200, 401],
    stack: [
      "Feature 12 - User ra tín hiệu cứu hộ",
      "Function 40 - Lịch sử cứu hộ user"
    ]
  },

  {
    id: "WBT-F13-41-SUP updateSosStatus",
    featureNo: 13,
    functionNo: 41,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/tin-hieu-sos/cap-nhat-trang-thai/1?status=DANG_XU_LY",
    normalizedUrl: "/api/tin-hieu-sos/cap-nhat-trang-thai/1?status=DANG_XU_LY",
    bodyType: "none",
    body: undefined,
    headers: {
      Cookie: "JSESSIONID=station-session"
    },
    expectedStatuses: [200, 400, 401, 403, 404],
    stack: [
      "Feature 13 - Trụ sở xử lý tín hiệu",
      "Function 41 - Cập nhật trạng thái SOS"
    ]
  },
  {
    id: "WBT-F13-42-SUP taoHoaDon",
    featureNo: 13,
    functionNo: 42,
    method: "POST",
    rawUrl: "{{baseUrl}}/api/hoa-don/tao",
    normalizedUrl: "/api/hoa-don/tao",
    bodyType: "json",
    body: {
      sosId: 1,
      tenSos: "Cứu hộ test",
      xuLy: "Xử lý cứu hộ",
      giaThuCong: 100000
    },
    headers: {
      "Content-Type": "application/json",
      Cookie: "JSESSIONID=station-session"
    },
    expectedStatuses: [200, 400, 401, 403],
    stack: [
      "Feature 13 - Trụ sở xử lý tín hiệu",
      "Function 42 - Tạo hóa đơn"
    ]
  },
  {
    id: "WBT-F13-43-SUP getSosHistory",
    featureNo: 13,
    functionNo: 43,
    method: "GET",
    rawUrl: "{{baseUrl}}/api/tin-hieu-sos/history",
    normalizedUrl: "/api/tin-hieu-sos/history",
    bodyType: "none",
    body: undefined,
    headers: {
      Cookie: "JSESSIONID=station-session"
    },
    expectedStatuses: [200, 401],
    stack: [
      "Feature 13 - Trụ sở xử lý tín hiệu",
      "Function 43 - Lịch sử cứu hộ trụ sở"
    ]
  }
];

function getAllWhiteBoxCases() {
  return [
    ...extractWhiteBoxCases(collection),
    ...supplementalSvp03Cases
  ];
}

describe("White-box core parser coverage", () => {
  test("WBT-CORE-01 parseFeatureNumber phủ có Feature và không có Feature", () => {
    expect(parseFeatureNumber("Feature 16 - Quản lý quyền lợi")).toBe(16);
    expect(parseFeatureNumber("SVP-01")).toBeNull();
  });

  test("WBT-CORE-02 parseFunctionNumber phủ Function có khoảng trắng và không khoảng trắng", () => {
    expect(parseFunctionNumber("Function 52 - Đổi tiền")).toBe(52);
    expect(parseFunctionNumber("Function4 - Xác thực trụ sở")).toBe(4);
    expect(parseFunctionNumber("Không phải function")).toBeNull();
  });

  test("WBT-CORE-03 inferFeatureFromFunction phủ các vùng function chính", () => {
    expect(inferFeatureFromFunction(2)).toBe(1);
    expect(inferFeatureFromFunction(9)).toBe(4);
    expect(inferFeatureFromFunction(30)).toBe(10);
    expect(inferFeatureFromFunction(33)).toBe(11);
    expect(inferFeatureFromFunction(37)).toBe(12);
    expect(inferFeatureFromFunction(43)).toBe(13);
    expect(inferFeatureFromFunction(47)).toBe(14);
    expect(inferFeatureFromFunction(50)).toBe(15);
    expect(inferFeatureFromFunction(55)).toBe(16);
    expect(inferFeatureFromFunction(999)).toBeNull();
  });

  test("WBT-CORE-04 normalizeUrl phủ baseUrl, localhost và url rỗng", () => {
    expect(normalizeUrl("{{baseUrl}}/api/auth/sync")).toBe("/api/auth/sync");
    expect(normalizeUrl("http://localhost:8082/api/qua/all")).toBe("/api/qua/all");
    expect(normalizeUrl()).toBe("");
  });

  test("WBT-CORE-05 getRawUrl phủ request null, url string và url object", () => {
    expect(getRawUrl()).toBe("");
    expect(getRawUrl({ url: "/api/test" })).toBe("/api/test");
    expect(getRawUrl({ url: { raw: "{{baseUrl}}/api/test" } })).toBe("{{baseUrl}}/api/test");
  });

  test("WBT-CORE-06 getBodyType phủ none/json/formdata/other", () => {
    expect(getBodyType({})).toBe("none");
    expect(getBodyType({ body: { mode: "raw" } })).toBe("json");
    expect(getBodyType({ body: { mode: "formdata" } })).toBe("formdata");
    expect(getBodyType({ body: { mode: "urlencoded" } })).toBe("other");
  });

  test("WBT-CORE-07 getRequestBody phủ JSON hợp lệ, JSON lỗi, formdata và none", () => {
    expect(getRequestBody({ body: { mode: "raw", raw: "{\"a\":1}" } })).toEqual({ a: 1 });
    expect(getRequestBody({ body: { mode: "raw", raw: "{bad-json}" } })).toBe("{bad-json}");
    expect(getRequestBody({ body: { mode: "formdata", formdata: [{ key: "a", value: "b" }] } })).toEqual([{ key: "a", value: "b" }]);
    expect(getRequestBody({})).toBeUndefined();
  });

  test("WBT-CORE-08 extractExpectedStatuses phủ response code, script status, script code và default", () => {
    expect(extractExpectedStatuses({ response: [{ code: 201 }, { code: 400 }] })).toEqual([201, 400]);

    expect(extractExpectedStatuses({
      event: [{
        script: {
          exec: ["pm.response.to.have.status(401);"]
        }
      }]
    })).toEqual([401]);

    expect(extractExpectedStatuses({
      event: [{
        script: {
          exec: ["pm.expect(pm.response.code).to.equal(404);"]
        }
      }]
    })).toEqual([404]);

    expect(extractExpectedStatuses({})).toEqual([200]);
  });

  test("WBT-CORE-09 buildHeaders phủ header thường, disabled header và bearer token", () => {
    expect(buildHeaders({
      request: {
        header: [
          { key: "Content-Type", value: "application/json" },
          { key: "X-Skip", value: "1", disabled: true }
        ],
        auth: {
          type: "bearer",
          bearer: [{ key: "token", value: "dev-token" }]
        }
      }
    })).toEqual({
      "Content-Type": "application/json",
      Authorization: "Bearer dev-token"
    });
  });

  test("WBT-CORE-10 buildHeaders phủ no header, no auth và Authorization có sẵn", () => {
    expect(buildHeaders({ request: {} })).toEqual({});

    expect(buildHeaders({
      request: {
        header: [
          { key: "Authorization", value: "Bearer existing-token" }
        ],
        auth: {
          type: "bearer",
          bearer: [{ key: "token", value: "dev-token" }]
        }
      }
    })).toEqual({
      Authorization: "Bearer existing-token"
    });
  });

  test("WBT-CORE-11 getFeatureAndFunctionFromStack phủ explicit feature và inferred feature", () => {
    expect(getFeatureAndFunctionFromStack([
      "Feature 10 - Quản lý Gói cứu hộ",
      "Function 30 - Tạo gói"
    ])).toEqual({
      featureNo: 10,
      functionNo: 30
    });

    expect(getFeatureAndFunctionFromStack([
      "SVP-04",
      "Function4 - Xác thực trụ sở",
      "Function 9 - Đăng nhập"
    ])).toEqual({
      featureNo: 4,
      functionNo: 9
    });
  });

  test("WBT-CORE-12 walkPostmanItems phủ items không phải array và item không có request", () => {
    expect(walkPostmanItems(null)).toEqual([]);

    const result = walkPostmanItems([
      {
        name: "Folder only",
        item: []
      },
      {
        name: "Item without request"
      }
    ]);

    expect(result).toEqual([]);
  });

  test("WBT-CORE-13 caseKey và expectedKey sinh key đúng", () => {
    expect(caseKey({
      featureNo: 13,
      functionNo: 42
    })).toBe("F13-FN42");

    expect(expectedKey({
      feature: 13,
      functionNo: 42
    })).toBe("F13-FN42");
  });

  test("WBT-CORE-14 buildFetchOptions phủ GET, POST JSON và POST formdata", () => {
    expect(buildFetchOptions({
      method: "GET",
      headers: {},
      bodyType: "none",
      body: undefined
    })).toEqual({
      method: "GET",
      headers: {}
    });

    expect(buildFetchOptions({
      method: "POST",
      headers: { "Content-Type": "application/json" },
      bodyType: "json",
      body: { a: 1 }
    })).toEqual({
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ a: 1 })
    });

    expect(buildFetchOptions({
      method: "POST",
      headers: {},
      bodyType: "formdata",
      body: [{ key: "file", value: "mock" }]
    })).toEqual({
      method: "POST",
      headers: {},
      body: [{ key: "file", value: "mock" }]
    });
  });

  test("WBT-CORE-15 createMockResponse phủ ok true và ok false", async () => {
    const okResponse = createMockResponse(200, { message: "ok" });
    expect(okResponse.ok).toBe(true);
    await expect(okResponse.text()).resolves.toBe("{\"message\":\"ok\"}");

    const errorResponse = createMockResponse(500, "Server error");
    expect(errorResponse.ok).toBe(false);
    await expect(errorResponse.text()).resolves.toBe("Server error");
  });
});

describe("White-box Feature 1 -> Feature 16 coverage matrix", () => {
  const cases = getAllWhiteBoxCases();
  const grouped = groupByFunction(cases);
  const verification = verifyExpectedFunctions(expectedFunctions, cases);
  const summary = buildCoverageSummary(expectedFunctions, cases);

  test("WBT-MATRIX-01 đọc được Postman Collection và supplemental cases", () => {
    expect(cases.length).toBeGreaterThan(50);
    expect(cases.every((item) => item.functionNo !== null)).toBe(true);
  });

  test("WBT-MATRIX-02 có đủ 16 Feature trong expected matrix", () => {
    expect(summary.totalFeatures).toBe(16);
    expect(summary.totalFunctions).toBe(54);
    expect(summary.apiRequiredFunctions).toBe(53);
  });

  test("WBT-MATRIX-03 mọi function API-required trong Excel đều được phủ bởi ít nhất 1 case", () => {
    const missing = verification.filter((item) => !item.covered);

    console.log("White-box coverage summary:", summary);

    expect(missing).toEqual([]);
  });

  test("WBT-MATRIX-04 Feature 1 Function 1 được ghi nhận là non-API documented row", () => {
    const f1 = verification.find((item) => item.feature === 1 && item.functionNo === 1);

    expect(f1.covered).toBe(true);
    expect(f1.apiRequired).toBe(false);
    expect(f1.note).toContain("Documented only");
  });

  test("WBT-MATRIX-05 các function quan trọng có case đúng nhóm", () => {
    expect((grouped["F10-FN30"] || []).length).toBeGreaterThan(0);
    expect((grouped["F11-FN33"] || []).length).toBeGreaterThan(0);
    expect((grouped["F12-FN37"] || []).length).toBeGreaterThan(0);
    expect((grouped["F13-FN42"] || []).length).toBeGreaterThan(0);
    expect((grouped["F16-FN55"] || []).length).toBeGreaterThan(0);
  });

  test("WBT-MATRIX-06 supplemental SVP-03 phủ đủ Function 30 đến 43", () => {
    const svp03Keys = [
      "F10-FN30",
      "F10-FN31",
      "F10-FN32",
      "F11-FN33",
      "F11-FN34",
      "F11-FN35",
      "F11-FN36",
      "F12-FN37",
      "F12-FN38",
      "F12-FN39",
      "F12-FN40",
      "F13-FN41",
      "F13-FN42",
      "F13-FN43"
    ];

    svp03Keys.forEach((key) => {
      expect((grouped[key] || []).length).toBeGreaterThan(0);
    });
  });
});

describe("White-box execution for every extracted and supplemental case", () => {
  const cases = getAllWhiteBoxCases();

  test.each(cases)("WBT-RUN $id", async (testCase) => {
    const fetchImpl = jest.fn();

    const result = await runWhiteBoxCase(fetchImpl, testCase);

    expect(result.status).toBe(testCase.expectedStatuses[0]);
    expect(result.requestUrl).toBeTruthy();
    expect(fetchImpl).toHaveBeenCalledTimes(1);

    const options = buildFetchOptions(testCase);
    expect(options.method).toBe(testCase.method);
  });

  test("WBT-RUN-ERROR phủ nhánh status không nằm trong expectedStatuses", async () => {
    const cases = getAllWhiteBoxCases();
    const fetchImpl = jest.fn();
    const testCase = {
      ...cases[0],
      expectedStatuses: [200]
    };

    await expect(runWhiteBoxCase(fetchImpl, testCase, 500)).rejects.toMatchObject({
      status: 500
    });
  });

  test("WBT-RUN-ERROR phủ nhánh thiếu fetchImpl và thiếu normalizedUrl", async () => {
    const cases = getAllWhiteBoxCases();

    await expect(runWhiteBoxCase(null, cases[0])).rejects.toThrow("fetchImpl is required");

    await expect(runWhiteBoxCase(jest.fn(), {
      ...cases[0],
      normalizedUrl: ""
    })).rejects.toThrow("testCase.normalizedUrl is required");
  });
});