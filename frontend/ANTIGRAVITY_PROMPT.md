# Smart Finance — Antigravity Prompt

Copy and paste this entire prompt into Antigravity's chat (File → Open Folder → frontend). Work only inside this frontend folder. Do not ask for approval; write every file as one complete file.

## Project Context

Stack: React 19 + Vite 8 + Bootstrap 5 + Axios + React Router 7.
Backend: Spring Boot 4.1 running at http://localhost:8080 (CORS allows localhost:5173).
Auth: JWT stored in sessionStorage (never localStorage). /api/auth/me returns user profile.
All backend endpoints require Authorization: Bearer <token>, except public endpoints.

## Available Backend Endpoints

| Area | Method | URL | Description |
|------|--------|-----|-------------|
| Auth | POST | /api/auth/login | Login, returns token + user |
| Auth | POST | /api/auth/register | Register |
| Auth | GET | /api/auth/me | Current user profile |
| Auth | POST | /api/auth/logout | Logout |
| Meta | GET | /api/meta/currencies | Currency list (INR, USD, GBP) |
| Categories | GET | /api/categories | List categories (system + user) |
| Categories | POST | /api/categories | Create category |
| Categories | PUT | /api/categories/{id} | Update |
| Categories | DELETE | /api/categories/{id} | Delete |
| Transactions | GET | /api/transactions | Paginated list with filters |
| Transactions | POST | /api/transactions | Create |
| Transactions | PUT | /api/transactions/{id} | Update |
| Transactions | DELETE | /api/transactions/{id} | Delete |
| Statements | POST | /api/bank-statements/upload | Upload CSV/Excel |
| Statements | GET | /api/bank-statements | List uploads |
| Statements | DELETE | /api/bank-statements/{id} | Undo import |
| Finance | GET | /api/finance/monthly-summary | Monthly summary |
| Finance | GET | /api/finance/overview | Overall summary |

Transaction GET params: page, size, sortBy, direction, from, to, categoryId, type, minAmount, maxAmount, q, source.

## Rules

- Never use parseFloat/toFixed for money — use Intl.NumberFormat via utils/format.js (formatMoney).
- Never use localStorage for token — sessionStorage only (see utils/storage.js).
- Every page must have data-testid attributes for Playwright tests.
- Loading states on all async actions.
- Show server error messages via utils/errors.js (getErrorMessage, getFieldErrors).
- All async code must handle 401 (session expired) — AuthProvider already handles it.
- Build one file at a time as ONE complete file, never append.
- Run `npm run build` and `npm run lint` after each task. Output must show 0 errors.
- After each task, run `dir /s /b src` and verify all listed files appear.

## Tasks

### Task 1: Transaction API client
Create exactly: src/api/transactionApi.js
Exports: list(params), create(data), update(id, data), remove(id).
Use apiClient from ./client.

### Task 2: Categories API client
Create exactly: src/api/categoryApi.js
Exports: list(), create(data), update(id, data), remove(id).

### Task 3: Bank statement API client
Create exactly: src/api/statementApi.js
Exports: upload(files), list(), remove(id).

### Task 4: Finance API client
Create exactly: src/api/financeApi.js
Exports: monthlySummary(from, to), overview(from, to).

### Task 5: Transactions page (Stage 2a)
Create exactly: src/pages/Transactions.jsx
- Fetch transactions via transactionApi.list() on mount.
- Filter bar: date range (from/to), category dropdown, type filter (all/income/expense), min/max amount, keyword search.
- Table columns: Date, Description, Category, Type (badge), Amount (green for credit, red for debit), Balance, Source, Actions (Edit/Delete buttons with data-testid).
- Pagination controls below table (page/size controls).
- "Add Transaction" button opens TransactionModal.
- Each row: Edit opens modal with transaction data; Delete shows confirm then calls remove.
- data-testid attributes on table rows, pagination buttons, and action buttons.

### Task 6: TransactionModal component (Stage 2b)
Create exactly: src/components/TransactionModal.jsx
- Props: isOpen, onClose, transaction (null for add), onSubmit callback.
- Form: Category dropdown (from categories API), Description text, Amount number, Type (INCOME/EXPENSE), Payment Method text.
- Submit sends POST/PUT to transactionApi.create/update.
- Show validation errors from server fieldErrors under matching inputs.
- data-testid on submit button and each input.

### Task 7: Connect Dashboard to APIs (Stage 2c)
Modify: src/pages/Dashboard.jsx
- On mount, fetch GET /api/finance/overview for total income/expense/savings/count.
- Fetch GET /api/finance/monthly-summary for current month to populate stat cards.
- Replace hardcoded £ 0.00 with real formatted values using formatMoney(value, user?.currency).
- Keep showBalances toggle hiding amounts.
- Keep Quick Action cards linking to upload/manual entry.

### Task 8: Layout navigation update (Stage 2d)
Modify: src/components/Layout.jsx
- Add nav links: Transactions, Import Statement, Monthly Analysis (placeholder pages).
- Keep existing Dashboard link, Logout, user profile badge.

### Task 9: TransactionsPage integration (Stage 2e)
Modify: src/App.jsx
- Add import for Transactions page.
- Add protected route: path="/transactions" element={<Transactions />}.

### Task 10: Build and verify
Run: npm run build && npm run lint
Run: dir /s /b src
Verify all files exist, build passes, lint shows 0 errors.

### Task 11: Stage 1 E2E test in browser
Start backend in STS (Spring Boot App).
npm run dev in frontend.
Open Chrome at http://localhost:5173.
Register user → login → dashboard shows real data → navigate to transactions page → add transaction → see it in table → logout → redirected to login.

## Completion Checklist

- [ ] src/api/transactionApi.js created
- [ ] src/api/categoryApi.js created
- [ ] src/api/statementApi.js created
- [ ] src/api/financeApi.js created
- [ ] src/components/TransactionModal.jsx created
- [ ] src/pages/Transactions.jsx created
- [ ] src/pages/Dashboard.jsx updated with real API data
- [ ] src/components/Layout.jsx updated with nav links
- [ ] src/App.jsx updated with transactions route
- [ ] npm run build passes
- [ ] npm run lint shows 0 errors
- [ ] Browser test: register, login, add transaction, see in table, logout

## File List After Completion

src/
├── api/
│   ├── client.js
│   ├── transactionApi.js
│   ├── categoryApi.js
│   ├── statementApi.js
│   └── financeApi.js
├── auth/
│   ├── authContext.js
│   ├── AuthProvider.jsx
│   └── useAuth.js
├── components/
│   ├── Layout.jsx
│   ├── ProtectedRoute.jsx
│   └── TransactionModal.jsx
├── pages/
│   ├── Dashboard.jsx
│   ├── Login.jsx
│   ├── Register.jsx
│   ├── Transactions.jsx
│   └── NotFound.jsx
├── utils/
│   ├── storage.js
│   ├── errors.js
│   └── format.js
├── main.jsx
├── App.jsx
└── index.css