const calculateDividendPayouts = (stakers, amountUSDC) => ({
  amountUSDC,
  payouts: stakers.map(staker => ({
    stakerId: staker.id,
    address: staker.address,
    shares: staker.shares,
    amountUSDC: stakers.length === 1 ? amountUSDC : 0
  }))
});

module.exports = { calculateDividendPayouts };
