const assert = require('node:assert/strict');
const { request } = require('./helpers/request');
const app = require('../server');

async function run() {
  const root = await request(app, '/');
  assert.equal(root.response.status, 200);
  assert.equal(root.body.project, 'StellarFraction API Backend');

  const properties = await request(app, '/api/properties');
  assert.equal(properties.response.status, 200);
  assert.ok(Array.isArray(properties.body));

  const stakers = await request(app, '/api/stakers');
  assert.equal(stakers.response.status, 200);
  assert.ok(Array.isArray(stakers.body));

  const history = await request(app, '/api/history');
  assert.equal(history.response.status, 200);
  assert.ok(Array.isArray(history.body));

  const unauthorized = await request(app, '/api/distribute', {
    method: 'POST',
    body: { propertyId: 1, amountUSDC: 100, idempotencyKey: 'abc' }
  });
  assert.equal(unauthorized.response.status, 401);

  const authorized = await request(app, '/api/distribute', {
    method: 'POST',
    headers: { Authorization: 'Bearer test-token' },
    body: { propertyId: 1, amountUSDC: 100, idempotencyKey: 'smoke-1' }
  });
  assert.equal(authorized.response.status, 200);
  assert.ok(authorized.body.log);

  console.log('Smoke tests passed');
}

run().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
