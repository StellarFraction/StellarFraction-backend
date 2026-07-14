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

test('preserves six-decimal payouts below one cent', () => {
  const result = calculateDividendPayouts([
    { id: 1, address: 'GONE', shares: 1 },
    { id: 2, address: 'GTWO', shares: 3 }
  ], 0.000004);

  assert.deepEqual(result.payouts.map(payout => payout.amountUSDC), [0.000001, 0.000003]);
});

test('conserves the requested amount when rounding is required', () => {
  const result = calculateDividendPayouts([
    { id: 1, address: 'GONE', shares: 1 },
    { id: 2, address: 'GTWO', shares: 1 },
    { id: 3, address: 'GTHREE', shares: 1 }
  ], 10);
  const distributedMicroUSDC = result.payouts.reduce(
    (total, payout) => total + Math.round(payout.amountUSDC * 1_000_000),
    0
  );

  assert.equal(distributedMicroUSDC, 10_000_000);
});

test('rejects malformed dividend amounts', () => {
  const stakers = [{ id: 1, address: 'GONE', shares: 1 }];

  for (const amount of ['not-a-number', Infinity, -1, 0]) {
    assert.throws(
      () => calculateDividendPayouts(stakers, amount),
      { message: 'amountUSDC must be a positive finite number' }
    );
  }
});
