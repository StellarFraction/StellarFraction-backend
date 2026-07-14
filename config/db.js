// Simulated database state for StellarFraction properties and stakers
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

let processedTransactions = new Set();

const initialState = {
  properties: structuredClone(properties),
  stakers: structuredClone(stakers),
  dividendHistory: structuredClone(dividendHistory)
};

module.exports = {
  getProperties: () => properties,
  getPropertyById: (id) => properties.find(p => p.id === parseInt(id)),
  addProperty: (property) => {
    properties.push(property);
    return property;
  },
  getStakers: () => stakers,
  addStaker: (staker) => {
    stakers.push(staker);
    return staker;
  },
  updateStakers: (updatedList) => {
    stakers = updatedList;
  },
  getDividendHistory: () => dividendHistory,
  addDividendRecord: (record) => {
    dividendHistory.push(record);
    return record;
  },
  isTransactionProcessed: (idempotencyKey) => processedTransactions.has(idempotencyKey),
  markTransactionProcessed: (idempotencyKey) => {
    processedTransactions.add(idempotencyKey);
  },
  reset: () => {
    properties = structuredClone(initialState.properties);
    stakers = structuredClone(initialState.stakers);
    dividendHistory = structuredClone(initialState.dividendHistory);
    processedTransactions = new Set();
  }
};
