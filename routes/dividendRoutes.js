const express = require('express');
const router = express.Router();
const { asyncHandler } = require('../middleware/asyncHandler');
const { authMiddleware } = require('../middleware/authMiddleware');
const {
  getDividendHistory,
  previewDividends,
  distributeDividends
} = require('../controllers/dividendController');

router.get('/history', asyncHandler(getDividendHistory));
router.post('/distribute', asyncHandler(async (req, res, next) => {
  authMiddleware(req, res, next);
}), asyncHandler(distributeDividends));
router.post('/distribute/preview', asyncHandler(previewDividends));
router.post('/distribute', asyncHandler(authMiddleware), asyncHandler(distributeDividends));

module.exports = router;
