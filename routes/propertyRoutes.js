const express = require('express');
const router = express.Router();
const { getProperties, getPropertyById, createProperty } = require('../controllers/propertyController');

router.route('/')
  .get(getProperties)
  .post(createProperty);

router.route('/:id')
  .get(getPropertyById);

module.exports = router;
