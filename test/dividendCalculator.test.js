const test = require('node:test');
const assert = require('node:assert/strict');
const { calculateDividendPayouts } = require('../services/dividendCalculator');

test('allocates the full dividend to a single staker', () => {
  const result = calculateDividendPayouts([
    { id: 1, address: 'GONE', shares: 25 }
  ], 10);

  assert.equal(result.payouts[0].amountUSDC, 10);
});
