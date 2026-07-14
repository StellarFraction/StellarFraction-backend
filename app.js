require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { errorHandler } = require('./middleware/errorMiddleware');

const app = express();

app.use(cors({
  origin: process.env.CORS_ORIGIN || 'http://localhost:5173',
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());

app.use('/api/properties', require('./routes/propertyRoutes'));
app.use('/api/stakers', require('./routes/stakerRoutes'));
app.use('/api', require('./routes/dividendRoutes'));

app.get('/', (req, res) => {
  res.json({
    project: 'StellarFraction API Backend',
    status: 'online',
    version: '1.0.0',
    documentation: 'See README.md for list of endpoints'
  });
});

app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    timestamp: new Date()
  });
});

app.use(errorHandler);

module.exports = app;
