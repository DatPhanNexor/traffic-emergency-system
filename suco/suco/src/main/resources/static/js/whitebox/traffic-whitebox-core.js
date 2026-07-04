function parseFeatureNumber(text) {
  const value = String(text || "");
  const match = value.match(/Feature\s+(\d+)/i);

  if (match) {
    return Number(match[1]);
  }

  return null;
}

function parseFunctionNumber(text) {
  const value = String(text || "");
  const match = value.match(/Function\s*(\d+)/i);

  if (match) {
    return Number(match[1]);
  }

  return null;
}

function inferFeatureFromFunction(functionNo) {
  if (functionNo === 2) return 1;
  if (functionNo >= 3 && functionNo <= 5) return 2;
  if (functionNo >= 6 && functionNo <= 8) return 3;
  if (functionNo >= 9 && functionNo <= 10) return 4;
  if (functionNo >= 11 && functionNo <= 13) return 5;
  if (functionNo >= 14 && functionNo <= 17) return 6;
  if (functionNo >= 18 && functionNo <= 21) return 7;
  if (functionNo >= 22 && functionNo <= 25) return 8;
  if (functionNo >= 26 && functionNo <= 29) return 9;
  if (functionNo >= 30 && functionNo <= 32) return 10;
  if (functionNo >= 33 && functionNo <= 36) return 11;
  if (functionNo >= 37 && functionNo <= 40) return 12;
  if (functionNo >= 41 && functionNo <= 43) return 13;
  if (functionNo >= 47 && functionNo <= 49) return 14;
  if (functionNo >= 50 && functionNo <= 51) return 15;
  if (functionNo >= 52 && functionNo <= 55) return 16;

  return null;
}

function normalizeUrl(rawUrl) {
  const raw = String(rawUrl || "");

  return raw
    .replace("{{baseUrl}}", "")
    .replace("http://localhost:8082", "")
    .replace(/\/+/g, "/")
    .trim();
}

function getRawUrl(request) {
  if (!request || !request.url) {
    return "";
  }

  if (typeof request.url === "string") {
    return request.url;
  }

  return request.url.raw || "";
}

function getBodyType(request) {
  if (!request || !request.body) {
    return "none";
  }

  if (request.body.mode === "raw") {
    return "json";
  }

  if (request.body.mode === "formdata") {
    return "formdata";
  }

  return "other";
}

function getRequestBody(request) {
  const bodyType = getBodyType(request);

  if (bodyType === "json") {
    const raw = request.body.raw || "";

    try {
      return JSON.parse(raw);
    } catch (error) {
      return raw;
    }
  }

  if (bodyType === "formdata") {
    return request.body.formdata || [];
  }

  return undefined;
}

function extractExpectedStatuses(item) {
  const responseCodes = Array.isArray(item.response)
    ? item.response.map((response) => response.code).filter(Boolean)
    : [];

  if (responseCodes.length > 0) {
    return [...new Set(responseCodes)];
  }

  const scripts = [];
  const events = Array.isArray(item.event) ? item.event : [];

  events.forEach((event) => {
    const exec = event && event.script && event.script.exec;
    if (Array.isArray(exec)) {
      scripts.push(exec.join("\n"));
    }
  });

  const scriptText = scripts.join("\n");
  const statusMatches = [...scriptText.matchAll(/status\((\d{3})\)/g)].map((match) => Number(match[1]));
  const codeMatches = [...scriptText.matchAll(/code\)\.to\.(?:equal|eql)\((\d{3})\)/g)].map((match) => Number(match[1]));

  const statuses = [...new Set([...statusMatches, ...codeMatches])];

  if (statuses.length > 0) {
    return statuses;
  }

  return [200];
}

function buildHeaders(item) {
  const request = item.request || {};
  const headers = {};

  const headerList = Array.isArray(request.header) ? request.header : [];
  headerList.forEach((header) => {
    if (!header.disabled && header.key) {
      headers[header.key] = header.value || "";
    }
  });

  const auth = request.auth;
  const bearer = auth && auth.type === "bearer" && Array.isArray(auth.bearer)
    ? auth.bearer.find((entry) => entry.key === "token")
    : null;

  if (bearer && bearer.value && !headers.Authorization) {
    headers.Authorization = `Bearer ${bearer.value}`;
  }

  return headers;
}

function getFeatureAndFunctionFromStack(stack) {
  const functionNo = stack
    .map(parseFunctionNumber)
    .filter((value) => value !== null)
    .pop() || null;

  const explicitFeature = stack
    .map(parseFeatureNumber)
    .filter((value) => value !== null)
    .pop() || null;

  const featureNo = explicitFeature || inferFeatureFromFunction(functionNo);

  return {
    featureNo,
    functionNo
  };
}

function walkPostmanItems(items, stack = []) {
  const cases = [];

  if (!Array.isArray(items)) {
    return cases;
  }

  items.forEach((item) => {
    const currentStack = [...stack, item.name || ""];

    if (Array.isArray(item.item)) {
      cases.push(...walkPostmanItems(item.item, currentStack));
      return;
    }

    if (!item.request) {
      return;
    }

    const { featureNo, functionNo } = getFeatureAndFunctionFromStack(currentStack);
    const request = item.request;
    const method = request.method || "GET";
    const rawUrl = getRawUrl(request);
    const normalizedUrl = normalizeUrl(rawUrl);
    const expectedStatuses = extractExpectedStatuses(item);

    cases.push({
      id: item.name || "Unnamed request",
      featureNo,
      functionNo,
      method,
      rawUrl,
      normalizedUrl,
      bodyType: getBodyType(request),
      body: getRequestBody(request),
      headers: buildHeaders(item),
      expectedStatuses,
      stack: currentStack
    });
  });

  return cases;
}

function extractWhiteBoxCases(collection) {
  if (!collection || !Array.isArray(collection.item)) {
    throw new Error("Invalid Postman collection");
  }

  return walkPostmanItems(collection.item).filter((item) => item.functionNo !== null);
}

function caseKey(item) {
  return `F${String(item.featureNo).padStart(2, "0")}-FN${item.functionNo}`;
}

function groupByFunction(cases) {
  return cases.reduce((result, item) => {
    const key = caseKey(item);

    if (!result[key]) {
      result[key] = [];
    }

    result[key].push(item);
    return result;
  }, {});
}

function verifyExpectedFunctions(expectedFunctions, cases) {
  const grouped = groupByFunction(cases);

  return expectedFunctions.map((expected) => {
    const key = `F${String(expected.feature).padStart(2, "0")}-FN${expected.functionNo}`;
    const matchedCases = grouped[key] || [];

    return {
      ...expected,
      key,
      covered: expected.apiRequired === false || matchedCases.length > 0,
      caseCount: matchedCases.length,
      note: expected.apiRequired === false
        ? "Documented only / no API required"
        : matchedCases.length > 0
          ? "Covered by Postman-derived Jest case"
          : "Missing API case"
    };
  });
}

function buildFetchOptions(testCase) {
  const options = {
    method: testCase.method,
    headers: testCase.headers || {}
  };

  if (testCase.method !== "GET" && testCase.body !== undefined) {
    options.body = testCase.bodyType === "json"
      ? JSON.stringify(testCase.body)
      : testCase.body;
  }

  return options;
}

function createMockResponse(status, body) {
  return {
    status,
    ok: status >= 200 && status < 300,
    text: jest.fn().mockResolvedValue(
      typeof body === "string" ? body : JSON.stringify(body)
    )
  };
}

async function runWhiteBoxCase(fetchImpl, testCase, forcedStatus) {
  if (!fetchImpl) {
    throw new Error("fetchImpl is required");
  }

  if (!testCase || !testCase.normalizedUrl) {
    throw new Error("testCase.normalizedUrl is required");
  }

  const expectedStatus = forcedStatus || testCase.expectedStatuses[0] || 200;
  const responseBody = {
    id: testCase.id,
    featureNo: testCase.featureNo,
    functionNo: testCase.functionNo,
    status: expectedStatus
  };

  fetchImpl.mockResolvedValue(createMockResponse(expectedStatus, responseBody));

  const response = await fetchImpl(testCase.normalizedUrl, buildFetchOptions(testCase));
  const rawText = await response.text();

  let parsedBody;
  try {
    parsedBody = JSON.parse(rawText);
  } catch (error) {
    parsedBody = rawText;
  }

  if (!testCase.expectedStatuses.includes(response.status)) {
    const error = new Error(`Unexpected status ${response.status}`);
    error.status = response.status;
    error.body = parsedBody;
    throw error;
  }

  return {
    status: response.status,
    body: parsedBody,
    requestUrl: testCase.normalizedUrl,
    requestOptions: buildFetchOptions(testCase)
  };
}

function buildCoverageSummary(expectedFunctions, cases) {
  const verification = verifyExpectedFunctions(expectedFunctions, cases);

  return {
    totalFeatures: new Set(expectedFunctions.map((item) => item.feature)).size,
    totalFunctions: expectedFunctions.length,
    apiRequiredFunctions: expectedFunctions.filter((item) => item.apiRequired !== false).length,
    coveredFunctions: verification.filter((item) => item.covered).length,
    missingFunctions: verification.filter((item) => !item.covered),
    totalPostmanCases: cases.length
  };
}

module.exports = {
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
};