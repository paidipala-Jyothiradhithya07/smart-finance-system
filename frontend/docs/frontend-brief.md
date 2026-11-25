# Smart Finance: frontend build brief

## 1. Rules for the agent
- The workspace root is the frontend folder. Do not read, create or change anything outside it.
- JavaScript only (no TypeScript). React + Vite, React Router, Axios, Bootstrap 5 (npm package, CSS classes). No Redux, no other UI kit, no chart library yet. Ask before installing anything else.
- Section 4 is the only source of truth for the API. Do not invent endpoints or fields. If something is missing or unclear, stop and ask.
- Build one stage at a time (section 6). After each stage run "npm run build" and "npm run lint", fix the errors, then stop and report what you built, how to run it, and anything uncertain.
- Do not run git commands. Never log tokens, passwords or financial data. Never put real bank statements in the project.

## 2. Setup
- API base URL comes from import.meta.env.VITE_API_BASE_URL. File .env.development contains: VITE_API_BASE_URL=http://localhost:8080. Never write the URL anywhere else.
- The dev server must use port 5173 (vite.config.js: server: { port: 5173, strictPort: true }). The backend only allows the origins http://localhost:5173 and http://localhost:3000.
- Structure: src/api (client.js plus one file per area), src/auth (AuthContext, ProtectedRoute), src/components (layout, common), src/pages, src/utils (format.js, errors.js).
- Use ONE axios instance. Do NOT set a default Content-Type header: axios sets JSON for objects and the multipart boundary for FormData by itself.
- Routes: /login, /register, /dashboard, /transactions, /transactions/new, /transactions/:id/edit, /income, /expenses, /upload, /analysis, /settings. "/" redirects to /dashboard. Unknown paths show a not-found page. Everything except /login and /register requires a login.

## 3. Global behaviour
Auth and token
- On login store { token, expiresAt } in sessionStorage (expiresAt = Date.now() + expiresInSeconds * 1000) and keep the user object in AuthContext. Do not use localStorage.
- An axios request interceptor adds "Authorization: Bearer <token>" to every call except register, login and GET /api/meta/currencies.
- On app start, if a stored token exists and is not expired, call GET /api/auth/me to restore the user; if it fails, clear the token.
- A response interceptor: on 401 for a protected call, clear the token and go to /login with the notice "Your session has expired. Please log in again." After login, return to the page the user wanted.
- Logout: call POST /api/auth/logout, clear the token and the user, go to /login.

Money
- The API returns amounts as JSON numbers with two decimals. NEVER do arithmetic on money in JavaScript: no sums, no percentages, no toFixed, no parseFloat on API values. Show the numbers exactly as the API gives them. The API already provides every total, average and percentage.
- Format with Intl.NumberFormat using the user's currency. Locale: en-IN for INR, en-US for USD, en-GB for GBP.
- For the transaction form: validate the typed text against /^\d{1,13}(\.\d{1,2})?$/ and require a value above zero before sending it as a number.
- A value of null for a balance means "unknown": show "Not available" with a short tooltip, never 0.
- Positive savings are green and negative savings are red. Credit is green and Debit is red. Do not rely on colour alone: also show the words Credit/Debit and a + or - sign.

Dates
- API dates are plain strings: yyyy-MM-dd for days and yyyy-MM for months. Never call new Date("yyyy-MM-dd") (it shifts by timezone). Split the string, or use new Date(year, month - 1, day), then format as "05 Jan 2026".
- Use <input type="date"> for days (max = today for transactions) and <input type="month"> for months.

Errors
- Every API error body is { timestamp, status, error, message, path, fieldErrors }. fieldErrors is an object { fieldName: "message" } (empty when none).
- Show message in an alert. Show each fieldErrors entry under the input with that name.
- No response at all: show "Cannot reach the server. Check that the backend is running."
- Use the backend's message text as it is. Do not rewrite it.

UX
- Every page has: loading state (spinner), empty state (helpful text and a button), error state.
- Disable a submit button while its request is running. Ask for confirmation before any delete.
- Responsive: usable at 375 px width. Tables scroll horizontally inside a wrapper. Labels on every input, buttons are real buttons, alerts use role="alert".
- Text from bank statements is user data: render it as normal React text. Never use dangerouslySetInnerHTML.

Test ids (for Playwright later): data-testid on login-email, login-password, login-submit, register-submit, nav-logout, transactions-table, transaction-row-<id>, upload-file-input, upload-preview-button, upload-import-button, import-message, month-card-<yyyy-MM> (for example month-card-2026-01), overview-total-income.

## 4. API contract
All paths start with the base URL. JSON in and out. Every endpoint needs the token except register, login, health and meta/currencies.
user = { id, name, email, role, currency, createdAt }

### Auth and meta
- POST /api/auth/register  body { name, email, password, currency? }  ->  201 user. Rules: name max 100; email valid, max 150; password 8 to 72 characters with at least one letter and one digit. Errors: 400 (fieldErrors), 409 (email exists).
- POST /api/auth/login  body { email, password }  ->  200 { token, tokenType, expiresInSeconds, user }. Error: 401 "Invalid email or password".
- GET /api/auth/me  ->  user
- PUT /api/auth/me  body { name, currency }  ->  user. Changes only the name and the display currency. NO amounts are converted.
- POST /api/auth/logout  ->  204
- GET /api/meta/currencies (public)  ->  { defaultCurrency, currencies: [ { code, symbol, name } ] }

### Categories
- GET /api/categories?type=INCOME|EXPENSE  ->  [ { id, name, type, systemDefault } ]. type is INCOME, EXPENSE or BOTH. Filtering by INCOME returns INCOME and BOTH.
- POST /api/categories  body { name, type }  ->  201 category. Name max 60. 409 if the name exists.
- PUT /api/categories/{id}  body { name, type }  ->  category. 403 for system categories (systemDefault is true). 409 if used by transactions and the type changes.
- DELETE /api/categories/{id}  ->  204. 403 for system categories. 409 if in use.

### Transactions
- tx = { id, transactionDate, description, amount, type, categoryId, categoryName, paymentMethod, source, referenceNo, balanceAfter, bankStatementId, createdAt }
- type is CREDIT (income) or DEBIT (expense). source is MANUAL or BANK_STATEMENT.
- POST /api/transactions  and  PUT /api/transactions/{id}  body { transactionDate, description, amount, type, categoryId, paymentMethod?, referenceNo? }  ->  201 / 200 tx.
  Rules: date not in the future; description required, max 500; amount above 0 with at most 2 decimals; paymentMethod max 30; referenceNo max 100; the category must fit the type (CREDIT needs INCOME or BOTH, DEBIT needs EXPENSE or BOTH), otherwise 400 with a message.
  A BANK_STATEMENT transaction cannot change date, amount or type (400). Its category, description, payment method and reference can change. So in the edit form, disable date, amount and type for those rows and say why.
- GET /api/transactions/{id}  ->  tx.  404 if it is not yours or does not exist.
- DELETE /api/transactions/{id}  ->  204.
- GET /api/transactions  ->  { content: [tx], page, size, totalElements, totalPages, last }
  Query parameters (all optional): from, to (yyyy-MM-dd, both inclusive), categoryId, type, minAmount, maxAmount, q (text in description or reference, max 100 characters), source, page (starts at 0), size (1 to 100, default 20), sortBy (transactionDate, amount or createdAt; default transactionDate), direction (asc or desc; default desc).

### Bank statements
- Upload rules: multipart/form-data, one part named "files" repeated for each file. At most 5 files, each at most 5 MB, extensions .csv .xlsx .xls only. PDF is not supported yet.
- Build the request like this: const form = new FormData(); files.forEach(f => form.append("files", f)); then post the form. Do not set a Content-Type header.
- POST /api/bank-statements/preview  (dry run, saves nothing)  ->  200
  { filesReceived, filesReadable, files: [ { fileName, status ("OK" or "FAILED"), fileError, fileType, headerRow, detectedColumns (object: role -> header text), totalDataRows, validRows, invalidRows, skippedRows, periodStart, periodEnd, monthsDetected: [ { month, transactionCount } ], warnings: [string], errors: [ { row, column, message } ], errorsTruncated, sample: [ { row, date, description, amount, type, balance, reference } ] } ] }
- POST /api/bank-statements/upload  (real import)  ->  201 if at least one file was imported, otherwise 200. Both are success: always read the body.
  { filesReceived, filesImported, transactionsImported, duplicatesSkipped, invalidRows, monthsImported, message, files: [ { fileName, status, message, statementId, transactionsDetected, imported, duplicates, invalid, skipped, periodStart, periodEnd, monthsDetected, warnings, errors: [ { row, column, message } ], errorsTruncated, duplicateRows: [ { row, date, description, amount, type } ], duplicatesTruncated } ] }
  File status is one of: IMPORTED, IMPORTED_WITH_ERRORS, NO_NEW_TRANSACTIONS, ALREADY_IMPORTED, FAILED. One file can be FAILED while the others succeed.
  Request-level errors: 400 (for example more than 5 files, or a missing "files" part), 413 (too large).
- GET /api/bank-statements  ->  [ { id, fileName, status, totalRows, importedCount, duplicateCount, invalidCount, periodStart, periodEnd, uploadedAt } ], newest first. status is COMPLETED or COMPLETED_WITH_ERRORS.
- GET /api/bank-statements/{id}  ->  the same fields plus monthsDetected: [ { month, transactionCount } ]
- DELETE /api/bank-statements/{id}  ->  204. This removes ALL transactions that came from that statement (undo import). Say so in the confirmation.

### Finance
- month = { year, month, label, totalIncome, totalExpense, savings, openingBalance, closingBalance, balanceAvailable, openingBalanceSource, closingBalanceSource, transactionCount, warnings: [string] }
  openingBalance and closingBalance can be null. The sources are STATEMENT, CARRIED_FORWARD, CALCULATED or UNAVAILABLE. Savings is income minus expenses. It is NOT the account balance: always show both and label them separately.
- GET /api/finance/monthly-summary?from=yyyy-MM&to=yyyy-MM (both optional)  ->  { currency, from, to, months: [month] }. Without parameters it returns every month from the first to the last month with data. No data gives months: []. At most 60 months per request.
- GET /api/finance/monthly-summary/{year}/{month}  ->  month
- GET /api/finance/overview?from&to  ->  { currency, from, to, monthsInRange, monthsWithTransactions, transactionCount, totalIncome, totalExpense, totalSavings, averageMonthlyIncome, averageMonthlyExpense, averageMonthlySavings, highestSpendingMonth, highestIncomeMonth (each null or { month, label, amount }), savingsTrend (INCREASING, DECREASING, STABLE, MIXED or INSUFFICIENT_DATA), savingsChanges: [ { month, label, savings, changeFromPrevious, changePercent } ], openingBalance, closingBalance, warnings: [string] }
  changeFromPrevious and changePercent are null for the first month. changePercent is a percentage number such as 11.11, not a fraction.
- GET /api/finance/category-summary?from&to  ->  { currency, from, to, totalIncome, totalExpense, expenseCategories: [ { categoryId, category, amount, percentage, transactionCount } ], incomeCategories: [same] }, sorted by amount, largest first.

## 5. Pages
- Login: email and password. Link to register. Shows the session-expired notice when redirected.
- Register: name, email, password (show the rules), currency select filled from GET /api/meta/currencies with the default preselected. On success go to /login with a success notice (register returns no token).
- Layout: top navbar that collapses on mobile, or a sidebar. Links: Dashboard, Transactions, Income, Expenses, Upload statement, Monthly analysis, Settings. Show the user's name and a Logout button. Do not add links for pages that do not exist yet (no Budgets, Reports).
- Dashboard: four cards. Total balance = overview.closingBalance. Income, Expenses and Savings of the LATEST month = the last item of monthly-summary (no parameters), with that month's label in the card titles. Below the cards, a "Recent transactions" list (GET /api/transactions?size=5). Empty state when there is no data, with buttons to upload a statement or add a transaction. NO charts yet.
- Transactions: filter bar (from, to, category, type, min and max amount, keyword q, source), sort controls, page-size select (10, 20, 50), pagination driven by the page response, and a table: date, description, category, type badge, amount, source badge (Manual or Bank), payment method, Edit and Delete buttons. Debounce the keyword by 400 ms. Initialise all filters from the URL query parameters (names as in the API) and keep the URL updated, so other pages can link to a filtered view. Button "Add transaction".
- Add or edit transaction: date, description, amount, type (radio: Income = CREDIT, Expense = DEBIT), category (loaded with GET /api/categories?type=INCOME for CREDIT or EXPENSE for DEBIT, reloaded when the type changes), payment method, reference. Show fieldErrors and message errors. Edit rules as in section 4.
- Income and Expenses: the same list component as Transactions with the type fixed to CREDIT or DEBIT and a matching title. The add button preselects the type.
- Upload statement (/upload): choose up to 5 files (check type, size and count in the browser as a convenience; the server decides). Button "Preview" calls /preview and shows, per file: status, detected columns, header row, valid / invalid / skipped counts, period, months detected, warnings, an errors table (row, column, message), and the sample rows. Button "Import" calls /upload and shows import-message (the response message), then per file: a status badge, counts, warnings, errors, and the duplicate rows. Import is disabled until a preview has at least one readable file. A "View monthly analysis" button goes to /analysis. Below: the statement history table (GET /api/bank-statements) with a Delete button whose confirmation says all transactions imported from that statement will be removed. Refresh the history after an import or delete. Show every file status clearly, including FAILED, ALREADY_IMPORTED and NO_NEW_TRANSACTIONS (these arrive with HTTP 200).
- Monthly analysis (/analysis): from and to month inputs plus an "All data" button. An overview panel from /overview: totals, averages, highest spending month, highest income month, savings trend badge, and the savingsChanges list. One card per month (data-testid month-card-yyyy-MM): label, income, expenses, savings, opening balance, closing balance, transaction count, small text for the balance sources, a warnings list, and a "View transactions" link to /transactions with from and to set to the first and last day of that month. A switch to show the same data as a table. A category breakdown from /category-summary: expense categories with amount and percentage. Savings and balance must be visibly separate.
- Settings: profile form (name, email read-only, currency select) with the note "Changing the currency only changes the symbol shown. Amounts are not converted." Save with PUT /api/auth/me and refresh the user in AuthContext. A categories section: list (system categories read-only), add, edit and delete your own, showing the 409 messages.

## 6. Stages (build one, stop, wait)
- Stage 1: project config (vite port, env, folders), axios client and interceptors, AuthContext, ProtectedRoute, layout with navbar, Login, Register, not-found page, and a placeholder Dashboard. Check: registering shows the currency list; login lands on the dashboard; a refresh in the same tab keeps you logged in; a bad password shows the server message; deleting the token in the browser dev tools and clicking a link returns to /login; Logout works.
- Stage 2: Dashboard cards and recent list, Transactions list with filters, sorting, pagination and URL state, Add/Edit form, Delete with confirmation, Income and Expenses pages. Check: filters combine; an invalid form shows the server's field errors; deleting asks first; editing a bank transaction disables date, amount and type.
- Stage 3: the Upload page with preview, import, history and undo. Check: use the sample files in section 7.
- Stage 4: Monthly analysis. Check: the numbers match section 7 exactly.
- Stage 5: Settings with profile and categories, then a polish pass: responsive check at 375 px, labels and alerts, data-testids, a README.md in the frontend folder (how to install and run), and removal of unused code.

## 7. Reference data for checking
The user imports three sample statements (January 13 rows, February 12, March 12). The monthly analysis must then show:
- January 2026: income 50,000.00, expenses 32,000.00, savings 18,000.00, opening 20,000.00, closing 38,000.00, 13 transactions
- February 2026: 55,000.00, 35,000.00, 20,000.00, opening 38,000.00, closing 58,000.00, 12 transactions
- March 2026: 52,000.00, 31,000.00, 21,000.00, opening 58,000.00, closing 79,000.00, 12 transactions
- Overview: total income 157,000.00, total expenses 98,000.00, total savings 59,000.00, average monthly income 52,333.33, highest spending month February 2026, savings trend INCREASING, closing balance 79,000.00.
