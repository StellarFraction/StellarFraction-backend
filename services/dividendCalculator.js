const USDC_SCALE = 1_000_000;

const calculateDividendPayouts = (stakers, amountUSDC) => {
  const totalShares = stakers.reduce((total, staker) => total + staker.shares, 0);
  const amountInMicroUSDC = Math.round(amountUSDC * USDC_SCALE);
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
