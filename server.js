const express = require('express');
const cors = require('cors');
const { env } = require('./config/env');
const { errorHandler } = require('./middleware/errorMiddleware');

const app = express();
const PORT = env.PORT;

app.use(cors({
  origin: env.CORS_ORIGIN,
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());

app.use('/', require('./routes/rootRoutes'));
app.use('/health', require('./routes/healthRoutes'));
app.use('/api/properties', require('./routes/propertyRoutes'));
app.use('/api/stakers', require('./routes/stakerRoutes'));
app.use('/api', require('./routes/dividendRoutes'));

app.use(errorHandler);

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`[StellarFraction Backend] Running on http://localhost:${PORT}`);
  });
}

module.exports = app;
