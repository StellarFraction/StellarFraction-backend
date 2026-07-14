const db = require('../config/db');
const { parseNumber } = require('../middleware/validation');

const listProperties = () => db.getProperties();

const getProperty = (id) => db.getPropertyById(id);

const createProperty = (payload) => {
  const { name, location, tokenCode, issuer, description, apy, value, totalShares } = payload || {};

  if (!name || typeof name !== 'string' || !name.trim()) {
    throw new Error('Property name is required');
  }

  if (!tokenCode || typeof tokenCode !== 'string' || !tokenCode.trim()) {
    throw new Error('tokenCode is required');
  }

  if (!issuer || typeof issuer !== 'string' || !issuer.trim()) {
    throw new Error('issuer is required');
  }

  const newProperty = {
    id: db.getProperties().length + 1,
    name: name.trim(),
    location: typeof location === 'string' ? location.trim() : '',
    tokenCode: tokenCode.trim().toUpperCase(),
    issuer: issuer.trim(),
    description: typeof description === 'string' ? description.trim() : '',
    apy: parseNumber(apy, 0),
    value: parseNumber(value, 0),
    totalShares: Math.max(1, Math.round(parseNumber(totalShares, 1000000))),
    stakedShares: 0
  };

  db.addProperty(newProperty);
  return newProperty;
};

module.exports = { listProperties, getProperty, createProperty };
