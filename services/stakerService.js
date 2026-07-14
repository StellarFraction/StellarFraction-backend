const db = require('../config/db');
const { parseNumber } = require('../middleware/validation');

const listStakers = () => db.getStakers();

const registerStaker = (payload) => {
  const { name, shares, address } = payload || {};

  if (!name || typeof name !== 'string' || !name.trim()) {
    throw new Error('Staker name is required');
  }

  if (!address || typeof address !== 'string' || !address.trim()) {
    throw new Error('Stellar wallet address is required');
  }

  const newStaker = {
    id: db.getStakers().length + 1,
    name: name.trim(),
    shares: Math.max(0, parseNumber(shares, 0)),
    debt: 0,
    usdcBalance: 0,
    address: address.trim()
  };

  db.addStaker(newStaker);
  return newStaker;
};

module.exports = { listStakers, registerStaker };
