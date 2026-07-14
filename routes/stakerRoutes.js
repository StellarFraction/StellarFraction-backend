const express = require('express');
const router = express.Router();
const { getStakers, registerStaker } = require('../controllers/stakerController');

router.route('/')
  .get(getStakers)
  .post(registerStaker);

module.exports = router;
