# BugBounty Admin Lab

A local training application with `secure` and `vulnerable` profiles. Both use the same reporting and admin workflow. The vulnerable profile contains representative exercises for the [OWASP Top 10:2025](https://top10.owasp.org/); the secure profile applies a corresponding control. Each exercise illustrates one risk, not the whole category. This is not a production bug bounty service.

## Quick start

Requirements: Java 17 or newer, Maven, Node.js 22, and Docker Desktop.

From the project root:

```powershell
docker compose up -d postgres
.\scripts\run-backend.ps1 secure
```

In a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open [http://127.0.0.1:5173/](http://127.0.0.1:5173/).

To switch profiles, stop the backend, run `.\scripts\run-backend.ps1 vulnerable`, refresh the browser, and sign in again. The profiles use separate local databases. The vulnerable backend binds only to `127.0.0.1` and refuses another bind address. Do not forward or proxy it to a public address.

The local database listens only on `127.0.0.1:55432`. The password in `compose.yml` is for disposable training data. The runner generates random JWT and webhook signing keys for the secure profile when those environment variables are unset. Restarting the secure backend expires existing sessions.

## Demo accounts

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `admin-demo-123` | ADMIN |
| `triager` | `triager-demo-123` | TRIAGER |
| `alice` | `alice-demo-123` | RESEARCHER |
| `bob` | `bob-demo-123` | RESEARCHER |

These accounts are inserted only when `LAB_SEED=true` and the user table is empty. Do not reuse these credentials outside this local lab.

## Architecture

Controllers handle HTTP requests and responses. Services hold login, role, report, and access decisions. Repositories contain SQL. The model package holds values shared between layers; controller responses do not contain password hashes. Profile-specific search, content, and access implementations are selected at startup.

The frontend starts in `main.tsx`. `App.tsx` coordinates state, `api.ts` handles requests, `types.ts` defines shared data, and `components/` holds the screens.

## Existing exercises

### SQL injection

- Endpoint: `GET /api/reports/search?q=`.
- Vulnerable: `VulnerableReportSearch` concatenates the query into SQL.
- Secure: `SecureReportSearch` binds the query as a parameter and limits a researcher to their own reports.
- Demo: Sign in as admin in each profile. Searching `no-such-title-2468` returns no reports. Searching `' OR 1=1 --` returns all reports only in the vulnerable profile. Alice seeing Bob's report without this input is a separate access control flaw.
- Test: `ProfileBehaviorTest.sqlInjectionChangesSearchResultOnlyInVulnerableMode`.

### Stored XSS

- Entry: `POST /api/reports` and `POST /api/reports/{id}/comments`.
- Display: report detail and comments in `frontend/src/components/ReportDetail.tsx`.
- Vulnerable: content is stored unchanged and rendered as HTML.
- Secure: `ContentPolicy` sanitizes on write, React displays text, and the server sends a script restricting Content Security Policy header.
- Tests: `commentWriteStoresOrRemovesExecutableMarkup` and `contentSecurityPolicyIsPresentOnlyInSecureMode`.

### CSRF

- Endpoint: `POST /api/admin/users/{id}/role`.
- Vulnerable: a cookie authenticated form POST changes a role without a CSRF token.
- Secure: `X-XSRF-TOKEN` is required and the cookie uses `SameSite=Strict`.
- Tests: `roleChangeWithoutCsrfTokenIsProfileDependent` and `validCsrfTokenKeepsAdminActionWorking`.

### IDOR

- Endpoint: `GET /api/reports/{id}` and its comments.
- Vulnerable: ownership is not checked.
- Secure: researchers receive 404 for someone else's report or comments.
- Test: `reportAndCommentsRespectOwnershipOnlyInSecureMode`.

### Authentication

- Endpoint: `POST /api/auth/login`.
- Vulnerable: MD5 password hashes, distinct login errors, no attempt limit, a predictable signing key, and tokens without expiry.
- Secure: BCrypt, generic login errors, a local attempt limit, a configured signing key, 15 minute tokens, and an algorithm check.
- Tests: `authenticationErrorsAndHashesDiffer`, `repeatedFailuresAreLimitedOnlyInSecureMode`, `predictableLabKeyOnlyForgesVulnerableAdminSession`, and `userResponsesNeverExposePasswordHashes`.

## OWASP Top 10:2025 map

The admin screen has an OWASP panel with buttons for the new exercises. Use a fresh `NEW` report for the workflow and webhook exercises; a `CLOSED` report cannot be reused. Buttons that change roles or statuses change local training data. The profiles have separate databases.

| Category | Local exercise |
| --- | --- |
| A01 Broken Access Control | Alice requests Bob's report by ID; vulnerable returns it, secure returns 404. The CSRF exercise is another access control related browser action. |
| A02 Security Misconfiguration | Public debug route exposes test usernames and emails only in vulnerable mode. |
| A03 Software Supply Chain Failures | Vulnerable approves untrusted local component content; secure checks source, version, and content digest. |
| A04 Cryptographic Failures | Vulnerable uses MD5 for passwords; secure uses BCrypt. |
| A05 Injection | SQL injection in report search and stored XSS in reports and comments. |
| A06 Insecure Design | Vulnerable closes a `NEW` report without triage; secure requires triage first. |
| A07 Authentication Failures | Login error, throttling, and token differences described above. |
| A08 Software or Data Integrity Failures | Vulnerable accepts an unsigned triage webhook; secure requires HMAC-SHA256. |
| A09 Security Logging & Alerting Failures | Secure records a role promotion and alert; vulnerable records neither. |
| A10 Mishandling of Exceptional Conditions | Vulnerable closes a report when review fails; secure returns 503 and leaves it `TRIAGED`. |

### New exercise endpoints

- **A02:** `GET /api/lab/debug/users` needs no login. Vulnerable exposes test usernames and emails; secure returns 404. Open it in a private browser window to demonstrate public access.
- **A03:** `POST /api/admin/lab/components` submits inert local component bytes and a manifest. Vulnerable approves untrusted source, version, or content. Secure compares them with the trusted local fixture and checks SHA-256 of the submitted bytes. Admin can fetch that fixture at `GET /api/admin/lab/component-fixture`. No package is downloaded, installed, or run; fixture sources use reserved `.test` names.
- **A06:** `POST /api/admin/lab/reports/{id}/close` on a `NEW` report succeeds in vulnerable mode and returns 409 in secure mode. Triage first with `POST /api/admin/lab/reports/{id}/triage`; normal close then succeeds.
- **A08:** An unsigned `POST /api/lab/webhook/triage` changes a `NEW` report to `TRIAGED` in vulnerable mode. Secure mode returns 401 and changes nothing; a valid HMAC-SHA256 signature over the exact request bytes is accepted. This machine to machine endpoint uses its signature for authenticity and is exempt from cookie CSRF. Admin can inspect `GET /api/admin/lab/integration-events`.
- **A09:** Promoting Bob to ADMIN leaves no event in vulnerable mode. Secure mode writes `ROLE_CHANGED` and `ROLE_PROMOTION_ALERT`, visible at `GET /api/admin/lab/security-events`. Use **Reset Bob** afterward to restore his RESEARCHER role. This is a local audit and alert fixture, not an external monitoring service.
- **A10:** After triage, close with `simulateReviewFailure=true`. The local review gateway throws. Vulnerable mode swallows the failure and closes the report; secure mode returns 503 and leaves it `TRIAGED`.

## Tests

Run the backend suite:

```powershell
mvn test
```

The PostgreSQL tests use Testcontainers and require Docker. H2 tests cover the same requests. GitHub CI runs the full backend suite and builds the frontend with `npm ci` and `npm run build`.

## Limits

The login attempt counter lives in one JVM and is not shared across instances. Tests establish server side CSRF acceptance or rejection and stored content behavior; they do not by themselves prove a cross site browser attack or script execution in a browser. A green suite is evidence for tested cases, not a claim that every vulnerability is absent. Only the secure profile should be considered for deployment, after a separate deployment review.
