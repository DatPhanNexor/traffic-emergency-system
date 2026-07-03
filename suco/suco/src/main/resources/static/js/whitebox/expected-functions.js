const expectedFunctions = [
  { feature: 1, functionNo: 1, apiRequired: false, note: "Function 1 có trong Excel nhưng không có ITC/API rõ trong Postman" },
  { feature: 1, functionNo: 2, apiRequired: true },

  { feature: 2, functionNo: 3, apiRequired: true },
  { feature: 2, functionNo: 4, apiRequired: true },
  { feature: 2, functionNo: 5, apiRequired: true },

  { feature: 3, functionNo: 6, apiRequired: true },
  { feature: 3, functionNo: 7, apiRequired: true },
  { feature: 3, functionNo: 8, apiRequired: true },

  { feature: 4, functionNo: 9, apiRequired: true },
  { feature: 4, functionNo: 10, apiRequired: true },

  { feature: 5, functionNo: 11, apiRequired: true },
  { feature: 5, functionNo: 12, apiRequired: true },
  { feature: 5, functionNo: 13, apiRequired: true },

  { feature: 6, functionNo: 14, apiRequired: true },
  { feature: 6, functionNo: 15, apiRequired: true },
  { feature: 6, functionNo: 16, apiRequired: true },
  { feature: 6, functionNo: 17, apiRequired: true },

  { feature: 7, functionNo: 18, apiRequired: true },
  { feature: 7, functionNo: 19, apiRequired: true },
  { feature: 7, functionNo: 20, apiRequired: true },
  { feature: 7, functionNo: 21, apiRequired: true },

  { feature: 8, functionNo: 22, apiRequired: true },
  { feature: 8, functionNo: 23, apiRequired: true },
  { feature: 8, functionNo: 24, apiRequired: true },
  { feature: 8, functionNo: 25, apiRequired: true },

  { feature: 9, functionNo: 26, apiRequired: true },
  { feature: 9, functionNo: 27, apiRequired: true },
  { feature: 9, functionNo: 28, apiRequired: true },
  { feature: 9, functionNo: 29, apiRequired: true },

  { feature: 10, functionNo: 30, apiRequired: true },
  { feature: 10, functionNo: 31, apiRequired: true },
  { feature: 10, functionNo: 32, apiRequired: true },

  { feature: 11, functionNo: 33, apiRequired: true },
  { feature: 11, functionNo: 34, apiRequired: true },
  { feature: 11, functionNo: 35, apiRequired: true },
  { feature: 11, functionNo: 36, apiRequired: true },

  { feature: 12, functionNo: 37, apiRequired: true },
  { feature: 12, functionNo: 38, apiRequired: true },
  { feature: 12, functionNo: 39, apiRequired: true },
  { feature: 12, functionNo: 40, apiRequired: true },

  { feature: 13, functionNo: 41, apiRequired: true },
  { feature: 13, functionNo: 42, apiRequired: true },
  { feature: 13, functionNo: 43, apiRequired: true },

  { feature: 14, functionNo: 46, apiRequired: true },
  { feature: 14, functionNo: 47, apiRequired: true },
  { feature: 14, functionNo: 48, apiRequired: true },
  { feature: 14, functionNo: 49, apiRequired: true },

  { feature: 15, functionNo: 46, apiRequired: true },
  { feature: 15, functionNo: 50, apiRequired: true },
  { feature: 15, functionNo: 51, apiRequired: true },

  { feature: 16, functionNo: 52, apiRequired: true },
  { feature: 16, functionNo: 53, apiRequired: true },
  { feature: 16, functionNo: 54, apiRequired: true },
  { feature: 16, functionNo: 55, apiRequired: true }
];

function expectedKey(item) {
  return `F${String(item.feature).padStart(2, "0")}-FN${item.functionNo}`;
}

module.exports = {
  expectedFunctions,
  expectedKey
};