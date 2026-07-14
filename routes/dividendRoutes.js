const express = require('express');
const router = express.Router();
const { asyncHandler } = require('../middleware/asyncHandler');
const { getDividendHistory, distributeDividends } = require('../controllers/dividendController');

router.get('/history', asyncHandler(getDividendHistory));
router.post('/distribute', asyncHandler(distributeDividends));

module.exports = router;
