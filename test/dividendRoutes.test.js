const test = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const app = require('../app');
const db = require('../config/db');

test.beforeEach(() => db.reset());

test('GET /api/history returns dividend records', async () => {
  const response = await request(app).get('/api/history').expect(200);

  assert.equal(response.body.length, 2);
  assert.equal(response.body[0].propertyId, 1);
});

test('POST /api/distribute requires authorization', async () => {
  const response = await request(app)
    .post('/api/distribute')
    .send({ propertyId: 1, amountUSDC: 100, idempotencyKey: 'auth-test' })
    .expect(401);

  assert.equal(response.body.message, 'Missing or invalid authorization header');
});
