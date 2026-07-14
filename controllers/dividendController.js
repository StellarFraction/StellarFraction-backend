const { listDividendHistory, distributeDividends: distribute } = require('../services/dividendService');
const db = require('../config/db');
const { calculateDividendPayouts } = require('../services/dividendCalculator');

// @desc    Get dividend history logs
// @route   GET /api/history
// @access  Public
const getDividendHistory = (req, res) => {
  res.json(listDividendHistory());
};

// @desc    Preview proportional USDC dividend payouts without changing state
// @route   POST /api/distribute/preview
// @access  Public
const previewDividends = (req, res) => {
  const { propertyId, amountUSDC } = req.body;
  const property = db.getPropertyById(propertyId);

  if (!property) {
    res.status(404);
    throw new Error('Property not found');
  }

  const calculation = calculateDividendPayouts(db.getStakers(), amountUSDC);
  res.json({
    property: {
      id: property.id,
      name: property.name,
      tokenCode: property.tokenCode
    },
    ...calculation
  });
};

// @desc    Trigger proportional USDC dividend distribution to stakers
// @route   POST /api/distribute
// @access  Admin/Private
const distributeDividends = (req, res) => {
  const result = distribute(req.body || {});
  res.status(200).json({
    message: `Successfully distributed ${result.log.amountUSDC} USDC for property: ${result.property.name}`,
    log: result.log,
    stakers: result.stakers
  });
};

module.exports = {
  getDividendHistory,
  previewDividends,
  distributeDividends
};
