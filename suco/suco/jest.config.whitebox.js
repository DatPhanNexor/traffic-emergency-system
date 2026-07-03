module.exports = {
  testEnvironment: "node",

  testMatch: [
    "<rootDir>/src/test/js/whitebox/**/*.test.js"
  ],

  collectCoverage: true,

  collectCoverageFrom: [
    "<rootDir>/src/main/resources/static/js/whitebox/traffic-whitebox-core.js",
    "<rootDir>/src/main/resources/static/js/whitebox/expected-functions.js"
  ],

  coverageDirectory: "<rootDir>/coverage/whitebox-feature-01-16",

  coverageReporters: [
    "text",
    "text-summary",
    "html",
    "lcov",
    "json-summary"
  ],

  coverageThreshold: {
    global: {
      statements: 90,
      branches: 85,
      lines: 90,
      functions: 90
    }
  },

  clearMocks: true,
  resetModules: true
};