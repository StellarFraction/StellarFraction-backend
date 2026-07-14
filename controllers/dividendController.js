const db = require('../config/db');

// @desc    Get dividend history logs
// @route   GET /api/history
// @access  Public
const getDividendHistory = (req, res) => {
  res.json(db.getDividendHistory());
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

  // Create transaction log
  const txHash = Math.random().toString(16).substring(2, 10) + Math.random().toString(16).substring(2, 10);
  const newLog = {
    id: db.getDividendHistory().length + 1,
    timestamp: new Date().toISOString(),
    amountUSDC: parseFloat(amountUSDC),
    propertyId: parseInt(propertyId),
    txHash
  };
  db.addDividendRecord(newLog);

  // Proportional math logic (simulated Soroban contract indexer update)
  let stakersList = db.getStakers();
  const totalStaked = stakersList.reduce((acc, s) => acc + s.shares, 0);

  if (totalStaked === 0) {
    res.status(400);
    throw new Error('Cannot distribute dividends: no stakers are currently invested in this property');
  }

  let totalDistributed = 0;
  stakersList = stakersList.map((s, idx) => {
    const shareFraction = s.shares / totalStaked;
    let earnings = shareFraction * amountUSDC;

    // Round to 6 decimal places (USDC precision)
    earnings = Math.round(earnings * 1e6) / 1e6;

    // Assign dust from rounding to last staker to ensure total matches amountUSDC
    if (idx === stakersList.length - 1) {
      const dust = amountUSDC - totalDistributed;
      earnings += dust;
    }

    totalDistributed += earnings;

    return {
      ...s,
      usdcBalance: s.usdcBalance + earnings
    };
  });
  db.updateStakers(stakersList);

  db.markTransactionProcessed(idempotencyKey);

  res.status(200).json({
    message: `Successfully distributed ${amountUSDC} USDC for property: ${property.name}`,
    log: newLog,
    stakers: stakersList
  });
};

module.exports = {
  getDividendHistory,
  distributeDividends
};
