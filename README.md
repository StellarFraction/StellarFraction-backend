# StellarFraction Management API

Part of the **StellarFraction** ecosystem: The Node.js Express backend API that coordinates property catalog indexing, tracks stakers balances, and monitors USDC dividend payment events.

---

## 🌐 StellarFraction Ecosystem Architecture

StellarFraction relies on a multi-tier decentralized setup where property deed tokens (issued as custom classic Stellar assets) and USDC dividend payouts (managed via Soroban contracts) are synced with a Node.js management API.

```
       +-------------------------------------------------+
       |             Client Browser (React UI)           |
       +-------+--------------------+----------------+---+
               |                    |                |
   (Wallet Connection)       (API Requests)    (SDK Triggers)
               |                    |                |
               v                    v                v
       +-------+-------+     +------+------+   +-----+------+
       |   Freighter   |     |   Node.js   |   |   Stellar  |
       |  / Albedo     |     |   Backend   |   |  Horizon/  |
       |  Wallet       |     |   API       |   |  Soroban   |
       +-------+-------+     +------+------+   +-----+------+
               |                    |                |
         (Signs Tx)            (DB Queries)     (Dividend Dist)
               |                    |                |
               v                    +--------------> |
   +-----------+-------------------------------------+-----------+
   |                       Stellar Network                       |
   |   - Property Deed Tokens (Classic Asset HORZ/OAKT/OMNI)     |
   |   - USDC Rental Dividend Distribution (Soroban Contract)    |
   +-------------------------------------------------------------+
```

---

## 💻 Role of this Repository

This repository hosts the **Node.js Express application** that coordinates database information with the blockchain.

### Key Responsibilities:
1. **Property Indexing:** Serves metadata for tokenized properties (Horizon Tower, Oakridge Tech Hub, Omni Retail Center) including APY values, valuation figures, and asset issuers.
2. **Staker Indexing:** Tracks user wallet details, mock USDC balances, and share ownership allocations.
3. **Dividend Event Monitoring:** Simulates or records webhooks when a landlord distributes rental income, updating staker balances according to proportional ratios.

---

## 🛠️ API Documentation & Endpoints

### 1. Property Catalog
* **`GET /api/properties`**: Serves the complete catalog of tokenized assets.
* **`GET /api/properties/:id`**: Serves parameters for a single property.

### 2. Stakers Directory
* **`GET /api/stakers`**: Retrieves list of registered stakers, share amounts, and balances.
* **`POST /api/stakers`**: Registers a new staker public key session.

### 3. Payout Transactions
* **`GET /api/history`**: Retrieves history logs of USDC dividend distributions.
* **`POST /api/distribute/preview`**: Previews proportional USDC payouts without changing balances or history.
* **`POST /api/distribute`**: Triggers a dividend payout, distributing USDC to stakers proportionally inside the pool.

#### Preview a dividend distribution

Send a property and a positive USDC amount. Preview requests do not require an
idempotency key because they never persist data.

```bash
curl -X POST http://localhost:5000/api/distribute/preview \
  -H 'Content-Type: application/json' \
  -d '{"propertyId":1,"amountUSDC":100}'
```

The response identifies the property, reports the eligible share total, and
lists each staker's percentage and six-decimal USDC payout. The
`totalDistributed` value always matches `amountUSDC` after rounding.

---

## 🚀 Setup & Local Execution

### Prerequisites
* **Node.js** (v18.0.0 or higher)
* **npm** (v9.0.0 or higher)

### Setup Commands

1. **Clone and navigate to the directory:**
   ```bash
   cd StellarFraction-backend
   ```

2. **Install all dependencies:**
   ```bash
   npm install
   ```

3. **Configure environment settings:**
   Create a `.env` file in the root directory (refer to `.env.example` if available):
   ```env
   PORT=5000
   ```

4. **Run the API server in development mode (with nodemon):**
   ```bash
   npm run dev
   ```
   *The server will boot locally at `http://localhost:5000/`.*

5. **Start the API server in production mode:**
   ```bash
   npm start
   ```

---

## 🤝 Contributing
Please consult [CONTRIBUTING.md](./CONTRIBUTING.md) for code formatting rules, middleware parameters, and pull request steps.

## 📄 License
This project is open-source under the terms of the MIT License. See [LICENSE](./LICENSE) for details.
