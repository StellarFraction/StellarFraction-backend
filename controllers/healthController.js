const getHealth = (req, res) => {
  res.json({
    status: 'UP',
    timestamp: new Date().toISOString(),
    uptimeSeconds: Math.round(process.uptime())
  });
};

module.exports = { getHealth };
