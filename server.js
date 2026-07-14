require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { errorHandler } = require('./middleware/errorMiddleware');

const app = express();
const PORT = process.env.PORT || 5000;

// Enable Middlewares
app.use(cors());
app.use(express.json());

// Routes Layer
app.use('/api/properties', require('./routes/propertyRoutes'));
app.use('/api/stakers', require('./routes/stakerRoutes'));
app.use('/api', require('./routes/dividendRoutes')); // Handles /api/history & /api/distribute

// Root Endpoint
app.get('/', (req, res) => {
  res.json({
    project: 'StellarFraction API Backend',
    status: 'online',
    version: '1.0.0',
    documentation: 'See README.md for list of endpoints'
  });
});

// Centralized Error Middleware
app.use(errorHandler);

// Boot Server
app.listen(PORT, () => {
  console.log(`[StellarFraction Backend] Running on http://localhost:${PORT}`);
});
