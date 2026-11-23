BugBounty Admin Lab
===================

A local training application with two profiles. Both profiles expose the same
reporting and admin workflow. The vulnerable profile deliberately exposes
representative risks for all ten OWASP Top 10:2025 categories; the secure
profile applies a corresponding control for each local exercise. These are
teaching examples, not complete coverage of a category or a production bug
bounty service. The comparison tests are the main evidence.

Quick start (PowerShell)
------------------------

Requirements: Java 17 or newer, Maven, Node.js 22, Docker Desktop.

1. In the project root, run: docker compose up -d postgres
2. In another terminal, run: .\scripts\run-backend.ps1 secure
3. In frontend, run: npm ci
4. In frontend, run: npm run dev
5. Open http://127.0.0.1:5173/

Stop the backend before switching profiles. To run the vulnerable profile,
replace "secure" in step 2 with "vulnerable". Refresh the browser and sign in
again. The profiles use separate local databases. The vulnerable backend binds
only to 127.0.0.1 and refuses another bind address. Do not forward or proxy it
to a public address.

The local database is exposed only on 127.0.0.1:55432. Its password in
compose.yml is for disposable training data. The run-backend.ps1 script creates
a new random JWT signing key for the secure profile when LAB_JWT_SECRET is not
set, so existing sessions expire when the process restarts.
The script also generates a separate random webhook signing key for the secure
profile. Never expose the vulnerable backend outside loopback.

Test-only accounts
------------------

admin    / admin-demo-123     ADMIN
triager  / triager-demo-123   TRIAGER
alice    / alice-demo-123     RESEARCHER
bob      / bob-demo-123       RESEARCHER

These accounts are inserted only when LAB_SEED=true and the user table is empty.
Do not reuse these credentials outside this local lab.

Behavior and evidence
---------------------

Controllers handle HTTP requests and responses. Services hold login, role,
report, and access decisions. Repositories contain SQL. The model package
holds values shared between those layers; controller responses never contain
password hashes. Profile-specific search, content, and access implementations
are selected at startup.
The frontend starts in main.tsx. App.tsx coordinates state, api.ts handles
requests, types.ts defines shared data, and components/ holds the screens.

SQL injection
  Endpoint: GET /api/reports/search?q=
  Vulnerable: VulnerableReportSearch concatenates q into SQL.
  Secure: SecureReportSearch binds q as a parameter and limits a
          researcher to their own reports.
  Demo: Sign in as admin in each profile. Searching no-such-title-2468
        returns no reports. Searching ' OR 1=1 -- returns all reports only
        in the vulnerable profile. Alice seeing Bob's report without this
        input is a separate access-control flaw, not SQL injection proof.
  Test: ProfileBehaviorTest.sqlInjectionChangesSearchResultOnlyInVulnerableMode

Stored XSS
  Entry: POST /api/reports and POST /api/reports/{id}/comments
  Display: report detail and comments in frontend/src/components/ReportDetail.tsx
  Vulnerable: content is stored unchanged and rendered as HTML.
  Secure: ContentPolicy sanitizes on write, React displays text, and the
          server sends a script-restricting Content-Security-Policy header.
  Test: ProfileBehaviorTest.commentWriteStoresOrRemovesExecutableMarkup
        and contentSecurityPolicyIsPresentOnlyInSecureMode

CSRF
  Endpoint: POST /api/admin/users/{id}/role
  Vulnerable: JWT is stored in a cookie; the role endpoint accepts a form
              POST without a CSRF token.
  Secure: X-XSRF-TOKEN is required and the cookie uses SameSite=Strict.
  Test: ProfileBehaviorTest.roleChangeWithoutCsrfTokenIsProfileDependent
        and validCsrfTokenKeepsAdminActionWorking

IDOR
  Endpoint: GET /api/reports/{id} and its comments
  Vulnerable: ownership is not checked.
  Secure: ReportService checks ownership before returning the report or
          comments; researchers get 404 for someone else's report.
  Test: ProfileBehaviorTest.reportAndCommentsRespectOwnershipOnlyInSecureMode

Authentication
  Endpoint: POST /api/auth/login
  Vulnerable: MD5 password hashes, distinct login errors, no attempt limit,
              predictable signing key, and token without an expiry.
  Secure: BCrypt, generic login error, local attempt limit, a configured
          signing key, a 15-minute token, and an algorithm check.
  Test: ProfileBehaviorTest.authenticationErrorsAndHashesDiffer
        and repeatedFailuresAreLimitedOnlyInSecureMode
        and predictableLabKeyOnlyForgesVulnerableAdminSession
        and userResponsesNeverExposePasswordHashes

OWASP Top 10:2025 exercise map
------------------------------

The admin screen includes an OWASP Top 10:2025 panel. Its buttons call the
endpoints below and show the result. Use a fresh NEW report for workflow and
webhook exercises; a CLOSED report cannot be reused. The two profiles have
separate databases, and buttons that change roles or statuses change local
training data. The test accounts and fixture source use reserved .test names.

  A01 Broken Access Control: Alice requests Bob's report by ID. Vulnerable
      returns it; secure returns 404. The CSRF exercise above is another
      access-control-related browser action.
  A02 Security Misconfiguration: GET /api/lab/debug/users needs no login.
      Vulnerable exposes test usernames/emails; secure returns 404. To prove
      the public exposure, open the URL in a private browser window.
  A03 Software Supply Chain Failures: POST /api/admin/lab/components submits
      local inert component bytes plus a manifest. Vulnerable approves
      untrusted source/version/content; secure checks the fixture's source,
      version and SHA-256 of the submitted bytes. No package is fetched,
      installed or run. The trusted fixture is available to admin at
      GET /api/admin/lab/component-fixture.
  A04 Cryptographic Failures: Vulnerable password hashes use MD5; secure uses
      BCrypt. The profile tests inspect stored hashes.
  A05 Injection: SQL injection in report search and stored XSS in report
      descriptions/comments are described above.
  A06 Insecure Design: POST /api/admin/lab/reports/{id}/close on a NEW report
      succeeds in vulnerable and returns 409 in secure. Triage it first with
      POST /api/admin/lab/reports/{id}/triage; normal close then succeeds.
  A07 Authentication Failures: distinct login errors, missing throttling and
      weak JWT construction in vulnerable are described above.
  A08 Software or Data Integrity Failures: POST /api/lab/webhook/triage with
      an unsigned JSON decision changes a NEW report to TRIAGED in vulnerable.
      Secure returns 401 and changes nothing; a valid HMAC-SHA256 signature
      over the exact request bytes is accepted. The webhook deliberately
      bypasses cookie CSRF because its signature is the machine-to-machine
      authenticity control. Admin can inspect /api/admin/lab/integration-events.
  A09 Security Logging & Alerting Failures: an admin promoting Bob to ADMIN
      leaves no security event in vulnerable. Secure writes ROLE_CHANGED and
      ROLE_PROMOTION_ALERT, visible at /api/admin/lab/security-events. This is
      a local audit/alert fixture, not an external monitoring service. Use the
      Reset Bob button afterward to restore his RESEARCHER role.
  A10 Mishandling of Exceptional Conditions: after triage, close with
      simulateReviewFailure=true. The local review gateway throws. Vulnerable
      swallows that failure and closes the report; secure returns 503 and
      leaves the report TRIAGED. This is a simulated dependency outage.

OWASP Top 10:2025: https://top10.owasp.org/

Run all backend tests with: mvn test
The PostgreSQL tests use Testcontainers and require Docker. The H2 profile
tests cover the same requests when Docker is unavailable. CI runs each
PostgreSQL profile separately and builds the frontend.

Limits
------

The login attempt counter lives in one JVM and is not shared across instances.
The tests show server-side CSRF acceptance/rejection and stored content, but
they do not by themselves prove a cross-site browser attack or script execution
in a browser. A green test suite is evidence for the tested cases, not a claim
that every vulnerability is absent. Only the secure profile should ever be
considered for deployment, after a separate deployment review.
