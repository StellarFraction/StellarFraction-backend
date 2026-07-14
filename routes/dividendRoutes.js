const express = require('express');
const router = express.Router();
const { asyncHandler } = require('../middleware/asyncHandler');
const { authMiddleware } = require('../middleware/authMiddleware');
const { getDividendHistory, distributeDividends } = require('../controllers/dividendController');

router.get('/history', asyncHandler(getDividendHistory));
router.post('/distribute', asyncHandler(async (req, res, next) => {
  authMiddleware(req, res, next);
}), asyncHandler(distributeDividends));

module.exports = router;
