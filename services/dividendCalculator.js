const USDC_SCALE = 1_000_000;

const calculateDividendPayouts = (stakers, amountUSDC) => {
  const numericAmount = Number(amountUSDC);
  if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
    const error = new Error('amountUSDC must be a positive finite number');
    error.statusCode = 400;
    throw error;
  }

  const totalShares = stakers.reduce((total, staker) => total + staker.shares, 0);
  if (!Number.isFinite(totalShares) || totalShares <= 0) {
    const error = new Error('Cannot calculate dividends without eligible shares');
    error.statusCode = 400;
    throw error;
  }

  const amountInMicroUSDC = Math.round(numericAmount * USDC_SCALE);
  const allocations = stakers.map(staker =>
    Math.floor(amountInMicroUSDC * staker.shares / totalShares)
  );
  const allocated = allocations.reduce((total, allocation) => total + allocation, 0);

  if (allocations.length > 0) {
    allocations[allocations.length - 1] += amountInMicroUSDC - allocated;
  }

  return {
    amountUSDC: amountInMicroUSDC / USDC_SCALE,
    totalShares,
    totalDistributed: amountInMicroUSDC / USDC_SCALE,
    payouts: stakers.map((staker, index) => ({
      stakerId: staker.id,
      address: staker.address,
      shares: staker.shares,
      percentage: staker.shares / totalShares * 100,
      amountUSDC: allocations[index] / USDC_SCALE
    }))
  };
};

module.exports = { calculateDividendPayouts };
