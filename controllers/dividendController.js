const { listDividendHistory, distributeDividends: distribute } = require('../services/dividendService');
const db = require('../config/db');
const { calculateDividendPayouts } = require('../services/dividendCalculator');

const getDividendHistory = (req, res) => {
  res.json(listDividendHistory());
};

const distributeDividends = (req, res) => {
  const result = distribute(req.body || {});
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
  const { propertyId, amountUSDC, idempotencyKey } = req.body;

  if (!propertyId || !amountUSDC || amountUSDC <= 0) {
    res.status(400);
    throw new Error('Please specify a valid propertyId and positive amountUSDC');
  }

  if (!idempotencyKey) {
    res.status(400);
    throw new Error('idempotencyKey is required to prevent duplicate processing');
  }

  if (db.isTransactionProcessed(idempotencyKey)) {
    res.status(409);
    throw new Error('This transaction has already been processed');
  }

  const property = db.getPropertyById(propertyId);
  if (!property) {
    res.status(404);
    throw new Error('Property not found');
  }

  const stakers = db.getStakers();
  const calculation = calculateDividendPayouts(stakers, amountUSDC);
  const payoutsByStakerId = new Map(
    calculation.payouts.map(payout => [payout.stakerId, payout.amountUSDC])
  );
  const stakersList = stakers.map(staker => ({
    ...staker,
    usdcBalance: staker.usdcBalance + payoutsByStakerId.get(staker.id)
  }));

  const txHash = Math.random().toString(16).substring(2, 10) + Math.random().toString(16).substring(2, 10);
  const newLog = {
    id: db.getDividendHistory().length + 1,
    timestamp: new Date().toISOString(),
    amountUSDC: calculation.amountUSDC,
    propertyId: parseInt(propertyId),
    txHash
  };
  db.addDividendRecord(newLog);
  db.updateStakers(stakersList);

  db.markTransactionProcessed(idempotencyKey);

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
