const authMiddleware = (req, res, next) => {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    res.status(401);
    throw new Error('Missing or invalid authorization header');
  }

  const token = authHeader.substring(7);

  if (!token || token.length === 0) {
    res.status(401);
    throw new Error('Invalid token format');
  }

  req.user = { token };
  next();
};

module.exports = { authMiddleware };
