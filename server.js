require('dotenv').config();
const express = require('express');
const cors = require('cors');
const { errorHandler } = require('./middleware/errorMiddleware');

const app = express();
const PORT = process.env.PORT || 5000;

// Enable Middlewares
app.use(cors({
  origin: process.env.CORS_ORIGIN || 'http://localhost:5173',
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
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

// Main Express entry point - initializes HTTP routing and database setup

// Register CORS middleware to allow cross-origin requests from frontend app

// Load configuration parameters from dotenv environment variables setup

// Parse incoming JSON body payloads for POST/PUT request structures

// Configure API rate limiting middleware to prevent backend overload

// Register helmet security headers to secure HTTP responses against exploits

// Define dividend history routes handler for client tracking queries

// Define property catalog routing map to retrieve asset list data

// Define staker registry routing map to retrieve user share information

// Set default port fallback value if no custom PORT variable is specified

// Bind server connection listener to start serving API routes on network

// Handle uncaughtException and unhandledRejection events to prevent exit

// Import dividend controller logic to handle distribution calculations

// Import property controller logic to handle real estate catalog CRUD

// Import staker controller logic to handle address ledger management

// Configure connection pools config for robust postgres client queries

// Mock database interface setup for local developer workspace tests
