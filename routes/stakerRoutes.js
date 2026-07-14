const express = require('express');
const router = express.Router();
const { asyncHandler } = require('../middleware/asyncHandler');
const { getStakers, registerStaker } = require('../controllers/stakerController');

router.route('/')
  .get(asyncHandler(getStakers))
  .post(asyncHandler(registerStaker));

module.exports = router;
