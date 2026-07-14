const isNonEmptyString = (value) => typeof value === 'string' && value.trim().length > 0;

const parseNumber = (value, fallback = 0) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

const pick = (obj, keys) => keys.reduce((acc, key) => {
  if (Object.prototype.hasOwnProperty.call(obj, key)) {
    acc[key] = obj[key];
  }
  return acc;
}, {});

module.exports = { isNonEmptyString, parseNumber, pick };
