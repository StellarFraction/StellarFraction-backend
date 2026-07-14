const express = require('express');
const router = express.Router();
const { asyncHandler } = require('../middleware/asyncHandler');
const { getHealth } = require('../controllers/healthController');

router.get('/', asyncHandler(getHealth));

module.exports = router;
