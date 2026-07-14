const calculateDividendPayouts = (stakers, amountUSDC) => ({
  amountUSDC,
  payouts: stakers.map(staker => ({
    stakerId: staker.id,
    address: staker.address,
    shares: staker.shares,
    amountUSDC: 0
  }))
});

module.exports = { calculateDividendPayouts };
