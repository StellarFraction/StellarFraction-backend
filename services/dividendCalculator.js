const calculateDividendPayouts = (stakers, amountUSDC) => {
  const totalShares = stakers.reduce((total, staker) => total + staker.shares, 0);

  return {
    amountUSDC,
    payouts: stakers.map(staker => ({
      stakerId: staker.id,
      address: staker.address,
      shares: staker.shares,
      amountUSDC: amountUSDC * staker.shares / totalShares
    }))
  };
};

module.exports = { calculateDividendPayouts };
