const { listStakers, registerStaker: registerStakerRecord } = require('../services/stakerService');

const getStakers = (req, res) => {
  res.json(listStakers());
};

const registerStaker = (req, res) => {
  const newStaker = registerStakerRecord(req.body);
  res.status(201).json(newStaker);
};

module.exports = {
  getStakers,
  registerStaker
};
