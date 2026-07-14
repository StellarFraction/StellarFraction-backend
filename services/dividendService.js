const db = require('../config/db');
const { parseNumber } = require('../middleware/validation');

const listDividendHistory = () => db.getDividendHistory();

const distributeDividends = ({ propertyId, amountUSDC, idempotencyKey }) => {
  const parsedPropertyId = Number(propertyId);
  if (!Number.isInteger(parsedPropertyId) || parsedPropertyId <= 0) {
    throw new Error('Please specify a valid propertyId');
  }

  const amount = parseNumber(amountUSDC, 0);
  if (!Number.isFinite(amount) || amount <= 0) {
    throw new Error('Please specify a positive amountUSDC');
  }

  if (!idempotencyKey || typeof idempotencyKey !== 'string' || !idempotencyKey.trim()) {
    throw new Error('idempotencyKey is required to prevent duplicate processing');
  }

  if (db.isTransactionProcessed(idempotencyKey)) {
    throw new Error('This transaction has already been processed');
  }

  const property = db.getPropertyById(parsedPropertyId);
  if (!property) {
    throw new Error('Property not found');
  }

  const txHash = `${Date.now().toString(16)}-${Math.random().toString(16).slice(2, 10)}`;
  const newLog = {
    id: db.getDividendHistory().length + 1,
    timestamp: new Date().toISOString(),
    amountUSDC: amount,
    propertyId: parsedPropertyId,
    txHash
  };

  db.addDividendRecord(newLog);

  const stakersList = db.getStakers();
  const totalStaked = stakersList.reduce((acc, s) => acc + (Number(s.shares) || 0), 0);

  if (totalStaked <= 0) {
    throw new Error('Cannot distribute dividends: no stakers are currently invested in this property');
  }

  let totalDistributed = 0;
  const updatedStakers = stakersList.map((staker, idx) => {
    const shareFraction = (Number(staker.shares) || 0) / totalStaked;
    let earnings = shareFraction * amount;
    earnings = Math.round(earnings * 1e6) / 1e6;

    if (idx === stakersList.length - 1) {
      earnings = amount - totalDistributed;
    }

    totalDistributed += earnings;
    return {
      ...staker,
      usdcBalance: (Number(staker.usdcBalance) || 0) + earnings
    };
  });

  db.updateStakers(updatedStakers);
  db.markTransactionProcessed(idempotencyKey);

  return { property, log: newLog, stakers: updatedStakers };
};

module.exports = { listDividendHistory, distributeDividends };
