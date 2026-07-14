const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

// In-Memory Database for Simulation
let properties = [
  {
    id: 1,
    name: 'The Horizon Tower',
    location: 'Austin, TX',
    tokenCode: 'HORZ',
    issuer: 'GA2XHORIZONTOWERISSUE3567890XYZTOWER1',
    description: 'Class-A commercial office skyscraper with long-term tech tenants. Stable occupancy and high rental yield.',
    apy: 8.5,
    value: 12500000,
    totalShares: 1500000,
    stakedShares: 1000000
  },
  {
    id: 2,
    name: 'Oakridge Tech Hub',
    location: 'Seattle, WA',
    tokenCode: 'OAKT',
    issuer: 'GB5ROAKRIDGETECHISSUE4567890XYZOAK2',
    description: 'Modern R&D flex-space laboratory. Strong tenants in green tech and artificial intelligence industries.',
    apy: 9.1,
    value: 8200000,
    totalShares: 950000,
    stakedShares: 500000
  },
  {
    id: 3,
    name: 'Omni Retail Center',
    location: 'Miami, FL',
    tokenCode: 'OMNI',
    issuer: 'GC8KOMNIRETAILISSUE5678901XYZOMNI3',
    description: 'High-traffic, grocery-anchored neighborhood retail shopping plaza with long lease structures.',
    apy: 8.9,
    value: 6400000,
    totalShares: 750000,
    stakedShares: 400000
  }
];

let stakers = [
  { id: 1, name: 'Alice (Investor A)', shares: 1000, debt: 0, usdcBalance: 0, address: 'GA...ALICE' },
  { id: 2, name: 'Bob (Investor B)', shares: 3000, debt: 0, usdcBalance: 0, address: 'GB...BOB' }
];

let dividendHistory = [
  { id: 1, timestamp: new Date(Date.now() - 86400000 * 3).toISOString(), amountUSDC: 500, propertyId: 1, txHash: '5c73...89ab' },
  { id: 2, timestamp: new Date(Date.now() - 86400000 * 10).toISOString(), amountUSDC: 1200, propertyId: 2, txHash: '2a14...78ef' }
];

// 1. Root Check
app.get('/', (req, res) => {
  res.json({ message: "StellarFraction Node.js backend is active", version: "1.0.0" });
});

// 2. Properties Endpoints
app.get('/api/properties', (req, res) => {
  res.json(properties);
});

app.get('/api/properties/:id', (req, res) => {
  const property = properties.find(p => p.id === parseInt(req.params.id));
  if (!property) return res.status(404).json({ error: "Property not found" });
  res.json(property);
});

// 3. Stakers Endpoints
app.get('/api/stakers', (req, res) => {
  res.json(stakers);
});

app.post('/api/stakers', (req, res) => {
  const { name, shares, address } = req.body;
  if (!name || !address) return res.status(400).json({ error: "Missing required staker details" });
  
  const newStaker = {
    id: stakers.length + 1,
    name,
    shares: parseFloat(shares) || 0,
    debt: 0,
    usdcBalance: 0,
    address
  };
  stakers.push(newStaker);
  res.status(201).json(newStaker);
});

// 4. Yield Distribution & Simulated Payments
app.get('/api/history', (req, res) => {
  res.json(dividendHistory);
});

app.post('/api/distribute', (req, res) => {
  const { propertyId, amountUSDC } = req.body;
  if (!propertyId || !amountUSDC || amountUSDC <= 0) {
    return res.status(400).json({ error: "Invalid distribution parameters" });
  }

  const property = properties.find(p => p.id === parseInt(propertyId));
  if (!property) return res.status(404).json({ error: "Property not found" });

  // Update history
  const txHash = Math.random().toString(16).substring(2, 10) + Math.random().toString(16).substring(2, 10);
  const newLog = {
    id: dividendHistory.length + 1,
    timestamp: new Date().toISOString(),
    amountUSDC: parseFloat(amountUSDC),
    propertyId: parseInt(propertyId),
    txHash
  };
  dividendHistory.push(newLog);

  // Distribute yield to stakers proportionally (O(1) simulated math)
  const totalStaked = stakers.reduce((acc, s) => acc + s.shares, 0);
  if (totalStaked > 0) {
    stakers = stakers.map(s => {
      const shareFraction = s.shares / totalStaked;
      const earnings = parseFloat((shareFraction * amountUSDC).toFixed(6));
      return {
        ...s,
        usdcBalance: s.usdcBalance + earnings
      };
    });
  }

  res.status(200).json({
    message: `Distributed ${amountUSDC} USDC for property: ${property.name}`,
    log: newLog,
    stakers
  });
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
