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
  const { propertyId, amountUSDC } = req.body;

  if (!propertyId || !amountUSDC || amountUSDC <= 0) {
    res.status(400);
    throw new Error('Please specify a valid propertyId and positive amountUSDC');
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

  if (totalStaked > 0) {
    stakersList = stakersList.map(s => {
      const shareFraction = s.shares / totalStaked;
      const earnings = parseFloat((shareFraction * amountUSDC).toFixed(6));
      return {
        ...s,
        usdcBalance: s.usdcBalance + earnings
      };
    });
    db.updateStakers(stakersList);
  }

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
