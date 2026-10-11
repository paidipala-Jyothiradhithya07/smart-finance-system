\# Smart Financial Management System: Requirements



A full-stack personal finance application. Users manage income, expenses and budgets, and can upload 2-3 months of bank statements (CSV/Excel). The system calculates income, expenditure, savings, opening balance and closing balance \*\*separately for each month\*\*.



\*\*Stack:\*\* Java 17, Spring Boot 4.1, Spring Data JPA/Hibernate, Spring Security + JWT, MySQL 8, Flyway, Maven, React, Bootstrap.



\---



\## Functional Requirements



| ID | Requirement |

|---|---|

| FR-1 | Register, login and logout using JWT; passwords hashed with BCrypt |

| FR-2 | CRUD for transactions (income and expense views are filtered views of the same data) |

| FR-3 | Category management (system defaults plus user-created) |

| FR-4 | Search and filter by date range, category, type, amount, keyword and source |

| FR-5 | Upload CSV/XLSX bank statements (multiple files allowed, e.g. 3 months) |

| FR-6 | Parse with flexible column mapping and normalize into one internal model |

| FR-7 | Validate rows; reject invalid ones with row-level errors, never silently drop them |

| FR-8 | Detect duplicate transactions, both within the file and against existing data |

| FR-9 | Rule-based automatic categorization (pluggable, so ML can be added later) |

| FR-10 | Per-month summary: income, expenses, savings, opening balance, closing balance, transaction count |

| FR-11 | Overall summary: totals, averages, highest income/spending month, savings trend |

| FR-12 | Monthly budgets with actual vs. budget, remaining amount, percentage used and warnings |

| FR-13 | Financial insights, with calculated facts clearly separated from suggestions |

| FR-14 | Charts: income vs. expense, savings trend, category split, spending trend, balance trend |

| FR-15 | Strict per-user data isolation |

| FR-16 | Configurable currency (default INR; USD and GBP also supported) |



\---



\## Non-Functional Requirements



\- \*\*Security:\*\* stateless JWT, BCrypt password hashing, every query scoped by `user\_id`, no entities exposed through the API, file type and size limits, no secrets in Git.

\- \*\*Correctness:\*\* all money uses `BigDecimal` in Java and `DECIMAL(15,2)` in MySQL. Never `double`.

\- \*\*Performance:\*\* composite indexes on `(user\_id, transaction\_date)`, batch inserts for imports, pagination on list endpoints.

\- \*\*Maintainability:\*\* layered monolith (Controller, Service, Repository), constructor injection, SOLID principles, parser and categorizer behind interfaces.

\- \*\*Reliability:\*\* a statement import runs in one transaction, so it fully succeeds or rolls back.

\- \*\*Usability:\*\* responsive UI, clear validation messages, loading states.

\- \*\*Testability:\*\* unit tests for the calculation logic, which is the most critical code.

\- \*\*Observability:\*\* SLF4J logging; full financial data and tokens are never logged.

\- \*\*Time handling:\*\* transaction dates are `LocalDate`, with no time zone conversions.



\---



\## Development Roadmap



| Phase | Deliverable |

|---|---|

| 1 | Requirements, architecture, database design, project setup, Git repository |

| 2 | Spring Boot and MySQL connection, Flyway migrations, entities, repositories |

| 3 | Registration, login, BCrypt, JWT filter, Spring Security |

| 4 | Transaction and category CRUD, DTOs, validation, global exception handler |

| 5 | Bank statement upload, parsers, normalization, duplicate detection, categorizer, import report |

| 6 | Monthly and overall analysis, balance resolver, SQL aggregation |

| 7 | React app: routing, auth, pages, upload UI, monthly analysis UI |

| 8 | Charts: income vs. expense, savings trend, category split, balance trend |

| 9 | Budgets and insights |

| 10 | Testing: JUnit 5, Mockito, MockMvc, Playwright |

| 11 | Security hardening, indexes, query optimization |

| 12 | Deployment, README, diagrams, resume and interview material |

