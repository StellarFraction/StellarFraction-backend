const express = require('express');
const router = express.Router();
const { getDividendHistory, distributeDividends } = require('../controllers/dividendController');

router.get('/history', getDividendHistory);
router.post('/distribute', distributeDividends);

module.exports = router;
