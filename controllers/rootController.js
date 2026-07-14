const getRootInfo = (req, res) => {
  res.json({
    project: 'StellarFraction API Backend',
    status: 'online',
    version: '1.0.0',
    documentation: 'See /api/docs for available endpoints'
  });
};

module.exports = { getRootInfo };
