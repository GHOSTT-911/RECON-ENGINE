# ReconEngine — Java (Spring Boot) Backend

Payment-gateway vs bank-settlement **reconciliation engine**. A Spring Boot 3.5 REST
backend (Java 17+, tested for Java 25) that stores transactions in a database, matches
gateway payouts against bank credits, explains every variance, and serves the
ReconEngine dashboard at `http://localhost:8081/`.

## Quick start (Windows)

1. Make sure **Java 17 or newer** is installed (`java -version`). You have Java 25 — fine.
   If not: install Java 21 from <https://adoptium.net/> (tick *Add to PATH*).
2. Double-click **`run.bat`**.
   - It checks Java, uses your Maven (or downloads Maven automatically if missing),
     starts the server and opens the dashboard in your browser.
   - The first run downloads libraries (~1–2 min). Later runs start in ~10 s.
3. Log in with **admin@reconengine.com / admin123** (pre-filled), or register a new account.

Stop the server with **Ctrl + C** in the black window.

**No MySQL needed.** By default the app uses an embedded H2 database saved at
`C:\Users\<you>\reconengine-data\` (kept outside OneDrive so syncing never locks it).
Your data survives restarts. 50 demo transactions are loaded on the first start.

### Command-line alternative
```bat
mvn spring-boot:run                    :: start (dev)
mvn clean package                      :: build + run tests -> target\reconengine.jar
java -jar target\reconengine.jar       :: run the built jar
set PORT=9090 && mvn spring-boot:run   :: use another port
```
Linux/macOS: `./run.sh`

## Using MySQL instead (optional)

Your original error (`Access denied for user 'root'@'localhost'`) meant the MySQL password
in `application.properties` did not match your MySQL server. Passwords are no longer
hard-coded. Double-click **`run-mysql.bat`** and type your MySQL username/password when
asked. The `reconengine` database and tables are created automatically.

Or set environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
and run `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`.

## Project structure
```
src/main/java/com/reconengine/
├── ReconciliationApplication.java     entry point
├── config/      ReconProperties (MDR rate, tolerance), WebConfig (CORS), DataSeeder
├── model/       Transaction, TransactionStatus, AppUser   (JPA entities)
├── repository/  TransactionRepository, AppUserRepository   (Spring Data JPA)
├── service/     MatchingEngine (rules), ReconciliationService, CsvTransactionParser, AuthService
├── controller/  ReconciliationController, AuthController, HealthController
├── dto/         request/response records
└── exception/   GlobalExceptionHandler (clean JSON errors)
src/main/resources/
├── application.properties             default (H2) config
├── application-mysql.properties       MySQL profile
└── static/index.html                  the dashboard (served at /)
src/test/java/...                      API integration tests (MockMvc)
```

## Matching rules
| Condition (gateway vs bank) | Status |
|---|---|
| amounts equal | `EXACT_MATCH` |
| gap equals the MDR fee (2% ± ₹0.05, configurable) | `PARTIAL_MATCH` |
| anything else | `UNRESOLVED_EXCEPTION` |
| closed by a reviewer | `RESOLVED_MANUAL` |

Change `recon.mdr-rate` / `recon.mdr-tolerance` in `application.properties`.
Money is handled with `BigDecimal` (no floating-point rounding errors).

## REST API (base `http://localhost:8081/api`)
| Method | Path | Purpose |
|---|---|---|
| GET  | `/health` | server + database status |
| GET  | `/reconcile/transactions[?status=UNRESOLVED_EXCEPTION]` | list transactions |
| GET  | `/reconcile/transactions/{id}` | one transaction |
| GET  | `/reconcile/summary` | totals, counts per status, match rate |
| POST | `/reconcile/run` | diagnose open exceptions ("Run AI Matcher") |
| POST | `/reconcile/reclassify` | re-apply matching rules to all rows |
| POST | `/reconcile/override` | body `{"id":"pay_RZP100049","action":"CHARGEBACK_FILED"}` |
| POST | `/reconcile/bulk-approve-mdr` | body `["pay_RZP100049","pay_RZP100054"]` |
| POST | `/reconcile/upload` | multipart `file=<csv>` (`?replace=false` to append) |
| POST | `/reconcile/upload/details` | same, returns imported/skipped counts |
| POST | `/reconcile/reset` | restore the 50 demo transactions |
| POST | `/auth/register` | `{"name","email","password"}` |
| POST | `/auth/login` | `{"email","password"}` → token |
| GET  | `/auth/me` | header `Authorization: Bearer <token>` |

Override actions: `CHARGEBACK_FILED`, `FEE_TOLERANCE_APPROVED`, `BANK_ADJUSTMENT_ACCEPTED`.

### CSV format
Header names are flexible (`Transaction ID`/`id`, `UTR`, `Gateway Amount`/`gatewayAmt`,
`Bank Credit`/`bankAmt`); quoted values and `1,000.00` style amounts work.
Try `sample-settlement.csv` from the dashboard's upload button.

### Database browser
With the default H2 setup open <http://localhost:8081/h2-console>, JDBC URL
`jdbc:h2:file:~/reconengine-data/reconengine;MODE=MySQL;AUTO_SERVER=TRUE`, user `sa`, empty password.

## Troubleshooting
| Problem | Fix |
|---|---|
| `Port 8081 was already in use` | another copy is running — close it, or `set PORT=9090` before `run.bat` |
| `'java' is not recognized` | install Java 21 from adoptium.net with *Add to PATH*, reopen the window |
| Dashboard shows "Backend unreachable" | make sure the black server window is still running |
| Start over with fresh data | stop the app, delete `C:\Users\<you>\reconengine-data`, start again |
