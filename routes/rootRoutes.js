const express = require('express');
const router = express.Router();
const { asyncHandler } = require('../middleware/asyncHandler');
const { getRootInfo } = require('../controllers/rootController');

router.get('/', asyncHandler(getRootInfo));

module.exports = router;
