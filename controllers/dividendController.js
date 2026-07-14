const { listDividendHistory, distributeDividends: distribute } = require('../services/dividendService');

const getDividendHistory = (req, res) => {
  res.json(listDividendHistory());
};

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
  distributeDividends
};
