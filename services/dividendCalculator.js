const USDC_SCALE = 1_000_000;

const calculateDividendPayouts = (stakers, amountUSDC) => {
  const totalShares = stakers.reduce((total, staker) => total + staker.shares, 0);
  const amountInMicroUSDC = Math.round(amountUSDC * USDC_SCALE);

  return {
    amountUSDC,
    payouts: stakers.map(staker => ({
      stakerId: staker.id,
      address: staker.address,
      shares: staker.shares,
      amountUSDC: Math.floor(amountInMicroUSDC * staker.shares / totalShares) / USDC_SCALE
    }))
  };
};

module.exports = { calculateDividendPayouts };
