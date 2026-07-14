# StellarFraction Backend API

This repository contains the Node.js Express server backend for the **StellarFraction** real estate micro-investment platform. It coordinates property assets lists, tracks staker details, and registers dividend logs from rental payouts.

---

## API Endpoints

### 1. Properties
- `GET /api/properties`: Lists all tokenized properties.
- `GET /api/properties/:id`: Details for a specific property.

### 2. Stakers
- `GET /api/stakers`: Retrieves active stakers and balances.
- `POST /api/stakers`: Registers a new investor/staker session.

### 3. Yields
- `GET /api/history`: Payout history of dividends.
- `POST /api/distribute`: Triggers a rent dividend payout (in USDC) and allocates it proportionally to stakers.

---

## Getting Started

### Prerequisites
- **Node.js** (v18+)
- **npm** (v9+)

### Installation
1. Install node modules:
   ```bash
   npm install
   ```

2. Run in development mode (with nodemon):
   ```bash
   npm run dev
   ```

3. Start production server:
   ```bash
   npm start
   ```

## Contributing
Please refer to [CONTRIBUTING.md](./CONTRIBUTING.md) for details on code style, linting, formatting, and PR templates.

## License
MIT License - see [LICENSE](./LICENSE) file for details.
