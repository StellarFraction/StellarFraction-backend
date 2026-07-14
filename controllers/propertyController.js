const db = require('../config/db');

// @desc    Get all properties
// @route   GET /api/properties
// @access  Public
const getProperties = (req, res) => {
  res.json(db.getProperties());
};

// @desc    Get property by ID
// @route   GET /api/properties/:id
// @access  Public
const getPropertyById = (req, res) => {
  const property = db.getPropertyById(req.params.id);
  if (!property) {
    res.status(404);
    throw new Error('Property not found');
  }
  res.json(property);
};

// @desc    Create new property
// @route   POST /api/properties
// @access  Admin/Private
const createProperty = (req, res) => {
  const { name, location, tokenCode, issuer, description, apy, value, totalShares } = req.body;

  if (!name || !tokenCode || !issuer) {
    res.status(400);
    throw new Error('Please include name, tokenCode, and issuer');
  }

  const newProperty = {
    id: db.getProperties().length + 1,
    name,
    location: location || '',
    tokenCode,
    issuer,
    description: description || '',
    apy: parseFloat(apy) || 0.0,
    value: parseFloat(value) || 0,
    totalShares: parseInt(totalShares) || 1000000,
    stakedShares: 0
  };

  db.addProperty(newProperty);
  res.status(201).json(newProperty);
};

module.exports = {
  getProperties,
  getPropertyById,
  createProperty
};
