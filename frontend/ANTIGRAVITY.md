# Smart Finance — Antigravity Context File

This file describes the Smart Financial Management System frontend project for Antigravity's AI assistant. Paste this into Antigravity's chat context when working on the frontend.

---

## Project Overview

**Smart Financial Management System** — a full-stack personal finance web app where users register, log in, enter income/expenses manually, or upload 2–3 months of bank statements (CSV/Excel). The app calculates income, expenditure, savings, opening balance, and closing balance per month.

**Backend:** Spring Boot 4.1.1, Java 17, MySQL 8, Flyway, JWT auth  
**Frontend:** React 19 + Vite 8 + Bootstrap 5 + Axios + React Router 7  
**Frontend path:** `D:\Project_folder\smart-finance-system\frontend`

---

## Phase Status

| Phase | Status |
|-------|--------|
| 1-6 Backend complete | ✅ Done |
| 7 Frontend setup & pages | 🔄 In progress (Stage 1 done) |
| 8 Charts | ❌ Not started |
| 9 Budgets & insights | ❌ Not started |
| 10 Testing | ❌ Not started |
| 11 Security hardening | ❌ Not started |
| 12 Deployment | ❌ Not started |

---

## Backend API Contracts (Actual Code)

### Authentication
| Method | Endpoint | Request | Response |
|--------|----------|---------|----------|
| POST | `/api/auth/register` | `{name, email, password, currency}` | `LoginResponseDTO{token, tokenType, expiresInSeconds, user}` |
| POST | `/api/auth/login` | `{email, password}` | `LoginResponseDTO` |
| GET | `/api/auth/me` | — | `UserResponseDTO` |
| POST | `/api/auth/logout` | — | 200 OK |
| GET | `/api/meta/currencies` | — | `CurrencyOptionsResponse{codes:[INR, USD, GBP], defaultCode}` |

### Categories
| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| GET | `/api/categories` | — | `List<CategoryResponseDTO>` |
| POST | `/api/categories` | body: `CategoryRequestDTO` | `CategoryResponseDTO` |
| PUT | `/api/categories/{id}` | body: `CategoryRequestDTO` | `CategoryResponseDTO` |
| DELETE | `/api/categories/{id}` | — | 204 No Content |

### Transactions
| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| GET | `/api/transactions` | `page, size, sortBy, direction, from, to, categoryId, type, minAmount, maxAmount, q, source` | `PageResponseDTO<TransactionResponseDTO>` |
| POST | `/api/transactions` | body: `TransactionRequestDTO` | `TransactionResponseDTO` |
| GET | `/api/transactions/{id}` | — | `TransactionResponseDTO` |
| PUT | `/api/transactions/{id}` | body: `TransactionRequestDTO` | `TransactionResponseDTO` |
| DELETE | `/api/transactions/{id}` | — | 204 No Content |

### Bank Statements
| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| POST | `/api/bank-statements/upload` | files: List<MultipartFile> | `ImportResponse` |
| GET | `/api/bank-statements` | — | `List<BankStatementResponseDTO>` |
| GET | `/api/bank-statements/{id}` | — | `BankStatementDetailDTO` |
| DELETE | `/api/bank-statements/{id}` | — | 204 No Content |

### Finance
| Method | Endpoint | Params | Response |
|--------|----------|--------|----------|
| GET | `/api/finance/monthly-summary` | `from, to` (YYYY-MM) | `MonthlySummaryResponse{currency, months:[MonthlySummaryDTO]}` |
| GET | `/api/finance/overview` | `from, to` | `OverviewResponse{...}` |
| GET | `/api/finance/category-summary` | `from, to` | `CategorySummaryResponse` |
| GET | `/api/finance/insights` | `from, to` | `InsightsResponse` |

### Transaction GET params: page, size, sortBy, direction, from, to, categoryId, type, minAmount, maxAmount, q, source.

---

## DTO Shapes (Actual Backend)

### UserResponseDTO
```json
{
  "id": 1,
  "name": "Adithya",
  "email": "adithya@example.com",
  "role": "USER",
  "currency": "INR",
  "createdAt": "2026-01-01T00:00:00"
}
```

### LoginResponseDTO
```json
{
  "token": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": { /* UserResponseDTO */ }
}
```

### TransactionRequestDTO (send to backend)
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| transactionDate | LocalDate (ISO string) | Yes | Not future |
| description | String | Yes | Max 500 chars |
| amount | BigDecimal (number) | Yes | Positive |
| type | "CREDIT" or "DEBIT" | Yes | |
| categoryId | Long | Yes | Must exist |
| paymentMethod | String | No | Max 30 chars |
| referenceNo | String | No | Max 100 chars |

### TransactionResponseDTO (from backend)
```json
{
  "id": 1,
  "transactionDate": "2026-01-15",
  "description": "Salary credit",
  "amount": 50000.00,
  "type": "CREDIT",
  "categoryId": 1,
  "categoryName": "Salary",
  "paymentMethod": "UPI",
  "source": "MANUAL",
  "balanceAfter": 70000.00,
  "bankStatementId": null,
  "createdAt": "2026-01-15T10:30:00"
}
```

### MonthlySummaryDTO
```json
{
  "year": 2026,
  "month": 1,
  "label": "January 2026",
  "totalIncome": 50000.00,
  "totalExpense": 32000.00,
  "savings": 18000.00,
  "openingBalance": 20000.00,
  "closingBalance": 38000.00,
  "balanceAvailable": true,
  "openingBalanceSource": "STATEMENT",
  "closingBalanceSource": "STATEMENT",
  "transactionCount": 13,
  "warnings": []
}
```

### OverviewResponse
```json
{
  "currency": "INR",
  "from": "2026-01",
  "to": "2026-03",
  "monthsInRange": 3,
  "monthsWithTransactions": 3,
  "transactionCount": 37,
  "totalIncome": 157000.00,
  "totalExpense": 98000.00,
  "totalSavings": 59000.00,
  "averageMonthlyIncome": 52333.33,
  "averageMonthlyExpense": 32666.67,
  "averageMonthlySavings": 19666.67,
  "highestSpendingMonth": {"year":2026,"month":2,"label":"February 2026","amount":35000.00},
  "highestIncomeMonth": {"year":2026,"month":2,"label":"February 2026","amount":55000.00},
  "savingsTrend": "INCREASING",
  "savingsChanges": [],
  "openingBalance": 20000.00,
  "closingBalance": 79000.00,
  "warnings": []
}
```

### ImportResponse
```json
{
  "filesReceived": 1,
  "filesImported": 1,
  "transactionsImported": 37,
  "duplicatesSkipped": 0,
  "invalidRows": 0,
  "monthsImported": 3,
  "message": "Import completed successfully",
  "files": []
}
```

### BankStatementResponseDTO
```json
{
  "id": 1,
  "fileName": "statement-jan.csv",
  "status": "COMPLETED",
  "totalRows": 13,
  "importedCount": 13,
  "duplicateCount": 0,
  "invalidCount": 0,
  "periodStart": "2026-01-01",
  "periodEnd": "2026-01-31",
  "uploadedAt": "2026-01-31T10:00:00"
}
```

### Error Response Shape (all errors)
```json
{
  "timestamp": "2026-01-01T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/transactions",
  "fieldErrors": {
    "fieldName": "error message"
  }
}
```

---

## Frontend Architecture Rules

1. **Token storage:** `sessionStorage` only (never `localStorage`)
2. **Money:** Use `Intl.NumberFormat` via `utils/format.js` (`formatMoney(value, currency)`) — never `parseFloat`/`toFixed`
3. **Auth:** Every API call needs `Authorization: Bearer <token>` header (handled by `api/client.js` interceptor)
4. **401 handling:** `api/client.js` auto-clears session and shows session-expired notice
5. **Dates:** Use `LocalDate` ISO format (`yyyy-MM-dd`) for API; `formatDate()` for display
6. **data-testid:** All interactive elements need `data-testid` for Playwright tests
7. **Loading states:** All async actions show loading state

---

## Tasks

### Task 1: Transaction API client
Create exactly: `src/api/transactionApi.js`
Exports: `list(params)`, `create(data)`, `update(id, data)`, `remove(id)`.
Use `apiClient` from `./client`.

### Task 2: Categories API client
Create exactly: `src/api/categoryApi.js`
Exports: `list()`, `create(data)`, `update(id, data)`, `remove(id)`.

### Task 3: Bank statement API client
Create exactly: `src/api/statementApi.js`
Exports: `upload(files)`, `list()`, `remove(id)`.

### Task 4: Finance API client
Create exactly: `src/api/financeApi.js`
Exports: `monthlySummary(from, to)`, `overview(from, to)`.

### Task 5: Transactions page (Stage 2a)
Create exactly: `src/pages/Transactions.jsx`
- Fetch transactions via `transactionApi.list()` on mount.
- Filter bar: date range (from/to), category dropdown, type filter (all/income/expense), min/max amount, keyword search.
- Table columns: Date, Description, Category, Type (badge), Amount (green for credit, red for debit), Balance, Source, Actions (Edit/Delete buttons with data-testid).
- Pagination controls below table (page/size controls).
- "Add Transaction" button opens `TransactionModal`.
- Each row: Edit opens modal with transaction data; Delete shows confirm then calls remove.
- `data-testid` attributes on table rows, pagination buttons, and action buttons.

### Task 6: TransactionModal component (Stage 2b)
Create exactly: `src/components/TransactionModal.jsx`
- Props: `isOpen`, `onClose`, `transaction` (null for add), `onSubmit` callback.
- Form: Category dropdown (from categories API), Description text, Amount number, Type (INCOME/EXPENSE), Payment Method text.
- Submit sends POST/PUT to `transactionApi.create`/`update`.
- Show validation errors from server `fieldErrors` under matching inputs.
- `data-testid` on submit button and each input.

### Task 7: Connect Dashboard to APIs (Stage 2c)
Modify: `src/pages/Dashboard.jsx`
- On mount, fetch GET `/api/finance/overview` for total income/expense/savings/count.
- Fetch GET `/api/finance/monthly-summary` for current month to populate stat cards.
- Replace hardcoded `£ 0.00` with real formatted values using `formatMoney(value, user?.currency)`.
- Keep `showBalances` toggle hiding amounts.
- Keep Quick Action cards linking to upload/manual entry.

### Task 8: Layout navigation update (Stage 2d)
Modify: `src/components/Layout.jsx`
- Add nav links: Transactions, Import Statement, Monthly Analysis (placeholder pages).
- Keep existing Dashboard link, Logout, user profile badge.

### Task 6: TransactionModal component (Stage 2b)
Create exactly: `src/components/TransactionModal.jsx`
- Props: `isOpen`, `onClose`, `transaction` (null for add), `onSubmit` callback.
- Form: Category dropdown (from categories API), Description text, Amount number, Type (INCOME/EXPENSE), Payment Method text.
- Submit sends POST/PUT to `transactionApi.create`/`update`.
- Show validation errors from server `fieldErrors` under matching inputs.
- `data-testid` on submit button and each input.

### Task 6: TransactionsPage integration (Stage 2e)
Modify: `src/App.jsx`
- Add import for Transactions page.
- Add protected route: path="/transactions" element={<Transactions />}.

### Task 10: Build and verify
Run: `npm run build && npm run lint`
Run: `dir /s /b src`
Verify all files exist, build passes, lint shows 0 errors.

### Task 11: Stage 1 E2E test in browser
Start backend in STS (Spring Boot App).
`npm run dev` in frontend.
Open Chrome at `http://localhost:5173`.
Register user → login → dashboard shows real data → navigate to transactions page → add transaction → see it in table → logout → redirected to login.

---

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