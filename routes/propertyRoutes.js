const express = require('express');
const router = express.Router();
const { asyncHandler } = require('../middleware/asyncHandler');
const { getProperties, getPropertyById, createProperty } = require('../controllers/propertyController');

router.route('/')
  .get(asyncHandler(getProperties))
  .post(asyncHandler(createProperty));

router.route('/:id')
  .get(asyncHandler(getPropertyById));

module.exports = router;
