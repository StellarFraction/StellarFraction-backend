const { listProperties, getProperty, createProperty: createPropertyRecord } = require('../services/propertyService');

const getProperties = (req, res) => {
  res.json(listProperties());
};

const getPropertyById = (req, res) => {
  const property = getProperty(req.params.id);
  if (!property) {
    res.status(404);
    throw new Error('Property not found');
  }
  res.json(property);
};

const createProperty = (req, res) => {
  const newProperty = createPropertyRecord(req.body);
  res.status(201).json(newProperty);
};

module.exports = {
  getProperties,
  getPropertyById,
  createProperty
};
