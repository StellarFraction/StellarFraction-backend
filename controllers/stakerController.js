const db = require('../config/db');

// @desc    Get all active stakers
// @route   GET /api/stakers
// @access  Public
const getStakers = (req, res) => {
  res.json(db.getStakers());
};

// @desc    Register a new staker
// @route   POST /api/stakers
// @access  Public
const registerStaker = (req, res) => {
  const { name, shares, address } = req.body;

  if (!name || !address) {
    res.status(400);
    throw new Error('Please include name and Stellar wallet address');
  }

  const newStaker = {
    id: db.getStakers().length + 1,
    name,
    shares: parseFloat(shares) || 0,
    debt: 0,
    usdcBalance: 0,
    address
  };

  db.addStaker(newStaker);
  res.status(201).json(newStaker);
};

module.exports = {
  getStakers,
  registerStaker
};
