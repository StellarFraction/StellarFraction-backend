const test = require('node:test');
const assert = require('node:assert/strict');
const { calculateDividendPayouts } = require('../services/dividendCalculator');

test('allocates the full dividend to a single staker', () => {
  const result = calculateDividendPayouts([
    { id: 1, address: 'GONE', shares: 25 }
  ], 10);

  assert.equal(result.payouts[0].amountUSDC, 10);
});

test('allocates dividends in proportion to unequal shares', () => {
  const result = calculateDividendPayouts([
    { id: 1, address: 'GONE', shares: 1000 },
    { id: 2, address: 'GTWO', shares: 3000 }
  ], 100);

  assert.deepEqual(result.payouts.map(payout => payout.amountUSDC), [25, 75]);
});
