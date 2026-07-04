module.exports = {
  testEnvironment: "jsdom",

  testMatch: [
    "<rootDir>/src/test/js/svp03/**/*.test.js"
  ],

  collectCoverage: true,

  collectCoverageFrom: [
    "src/main/resources/static/js/svp03/**/*.js",
    "!**/node_modules/**"
  ],

  coverageDirectory: "coverage/svp03",

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