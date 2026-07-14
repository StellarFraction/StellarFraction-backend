const test = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const app = require('../app');

test('GET / reports that the API is online', async () => {
  const response = await request(app).get('/').expect(200);

  assert.equal(response.body.status, 'online');
  assert.equal(response.body.project, 'StellarFraction API Backend');
});

test('GET /health reports system availability', async () => {
  const response = await request(app).get('/health').expect(200);

  assert.equal(response.body.status, 'UP');
  assert.ok(!Number.isNaN(Date.parse(response.body.timestamp)));
});
