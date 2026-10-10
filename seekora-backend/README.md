# Seekora — Knowledge Search Demo (Java Servlet + JDBC + MySQL)

Full backend for the Seekora landing/product page: MySQL schema, JDBC DAO layer, service layer,
REST-style servlets, BCrypt authentication, session management and the original HTML page wired
to the API.

Packaging: WAR (`seekora.war`) for Tomcat 9. Imports into Eclipse as a Maven project /
Dynamic Web Project.

---

## 1. Requirements

| Tool | Version used |
|------|--------------|
| JDK | 17 (source/target 11, so JDK 11+ works) |
| Apache Tomcat | 9.x (javax.servlet, **not** Tomcat 10+) |
| MySQL Server | 8.0 |
| Maven | 3.6+ (optional if you build inside Eclipse) |
| Eclipse | Eclipse IDE for Enterprise Java and Web Developers |

> Tomcat 10/11 use the `jakarta.*` namespace and will **not** run this WAR.

---

## 2. Database setup

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

Run these commands only for a new local development database. `schema.sql` creates `seekora_db`
if it does not exist and does not drop existing databases or tables. It is an initial schema
script, not an upgrade migration. Back up and plan migrations before changing an existing
database. Run `seed.sql` only once on a fresh schema.
`seed.sql` loads plans, plan features, knowledge topics + sources, chat intents,
features, FAQs and platform stats. It no longer seeds fictional testimonials.

The React application saves its signed-in AI threads in the `conversations` and `messages`
tables. New databases get these tables from `schema.sql`. On an existing database, apply the
additive migration once:

```bash
mysql -u root -p < database/migrations/20261009_react_app_conversations.sql
```

For an existing database created from the earlier demo seed, apply the targeted content updates
once. This also suspends only the two bundled accounts with the publicly shared demo password;
other account records and user data are not changed.

```bash
mysql -u root -p seekora_db < database/migrations/20261009_truthful_demo_content.sql
```

Create an account from the Seekora sign-up dialog after deployment. New seed installs do not include shared demo login credentials.

### Connection settings

`src/main/resources/db.properties` contains the local URL and pool settings, but no credentials.
Set these environment variables in PowerShell before starting Tomcat:

```powershell
$env:SEEKORA_DB_URL = 'jdbc:mysql://localhost:3306/seekora_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8'
$env:SEEKORA_DB_USER = 'your_local_mysql_user'
$env:SEEKORA_DB_PASSWORD = 'your_local_mysql_password'
$env:GEMINI_API_KEY = 'your_gemini_api_key'
$env:SEEKORA_GEMINI_MODEL = 'gemini-3.8-flash' # optional; this is the default
$env:SEEKORA_ALLOWED_ORIGINS = 'https://your-ngrok-host.ngrok-free.dev'
```

The application requires a valid database URL, username, and password at startup. The URL
defaults to `db.url` in `db.properties`; set `SEEKORA_DB_URL` to override it. Set
`SEEKORA_DB_USER` and `SEEKORA_DB_PASSWORD` to your local MySQL credentials; there are no
credential defaults in the repository.

Create a Gemini API key in [Google AI Studio](https://aistudio.google.com/apikey). The key stays
on the Tomcat server, is sent to Google only in the `x-goog-api-key` request header, and is never
sent to the browser. Seekora calls Gemini's `generateContent` API with Google Search grounding for
general questions, chat, and research. Gemini is instructed to compare multiple independent websites
when available and synthesize their findings. The backend extracts answer text from candidate content
parts and citations from grounding chunks, returning only URLs Gemini supplies. The optional model setting
defaults to `gemini-3.8-flash`. The backend reads the key from `GEMINI_API_KEY`; Seekora's existing
MySQL search history and saved conversations remain unchanged. Google Search grounding may incur
Gemini API charges.

Chat answers open-ended questions too, with a limit of 20 assistant replies per browser session per
day. Search requests continue to follow account or guest daily search limits.

Cross-origin requests are disabled by default. For an HTTPS ngrok tunnel forwarding to local HTTP,
set `SEEKORA_ALLOWED_ORIGINS` to the exact public origin (no trailing slash), for example
`https://your-ngrok-host.ngrok-free.dev`.
Requests from origins outside that allowlist receive the normal JSON API error envelope with
HTTP 403; the allowlist is not disabled to make browser signup or search work.

---

## 3. Build and run from the command line

```bash
mvn package                             # produces target/seekora.war
cp target/seekora.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

In VS Code's PowerShell terminal on Windows, set the same environment variables above, then run:

```powershell
$env:CATALINA_HOME = 'C:\path\to\apache-tomcat-9.0.x'
$env:CATALINA_BASE = $env:CATALINA_HOME
mvn package
Copy-Item target/seekora.war "$env:CATALINA_HOME\webapps\seekora.war"
& "$env:CATALINA_HOME\bin\startup.bat"
```

Set `CATALINA_HOME` and the database / AI environment variables in the same PowerShell
terminal used to start Tomcat so the Tomcat process inherits them.

Open http://localhost:8080/seekora/

Health check: http://localhost:8080/seekora/api/health

## Deploy on Render without Tomcat

The backend WAR can run on Jetty 9 instead of Tomcat. The repository's
`render.yaml` and `Dockerfile` build and deploy it as a Render web service.
Create a Blueprint from the `Aditya1532007/Seekora.AI` repository and select
`seekora-backend/render.yaml`.

Render does not provide managed MySQL. Create a MySQL database with a provider
that allows connections from Render, run `database/schema.sql` and
`database/seed.sql`, and supply these environment variables to the service:

- `SEEKORA_DB_URL`, `SEEKORA_DB_USER`, and `SEEKORA_DB_PASSWORD`: connection
  details for the hosted database.
- `SEEKORA_ALLOWED_ORIGINS`: the exact HTTPS origin of the frontend, such as
  `https://seekora-frontend.onrender.com`.
- `GEMINI_API_KEY`: Google AI Studio key used by the server for AI answers.

Keep these values in Render's environment settings, not in Git. Once deployed,
check `https://seekora-api.onrender.com/api/health`; it returns healthy only
when the database connection is configured and available. The frontend's
Render Blueprint is in the separate `seekora_final_project` repository and
uses this service's `/api` endpoint.

---

## 4. Running it in Eclipse

1. **Import** — `File > Import… > Maven > Existing Maven Projects`, pick the `seekora-backend`
   folder, Finish. Eclipse resolves the dependencies and adds the Dynamic Web Module facet
   from the WAR packaging.
2. **Java version** — right-click the project > `Properties > Java Build Path > Libraries`,
   make sure the JRE System Library is JDK 11 or newer.
3. **Add the server** — `Window > Show View > Servers`, click the link to create one,
   choose `Apache > Tomcat v9.0 Server`, point it at your Tomcat installation directory.
4. **Deploy** — right-click the project > `Run As > Run on Server`, choose the Tomcat 9 server.
   Eclipse publishes to `/seekora-backend` by default; set the context root under
   `Properties > Web Project Settings` if you prefer `/seekora`. The frontend derives the API
   base URL from the context path, so either works.
5. **MySQL driver** — already a Maven dependency (`mysql-connector-j`), nothing to copy into
   `tomcat/lib`.
6. If Eclipse complains the project has no `WebContent` folder, that's expected: this project
   uses the Maven layout, `src/main/webapp`.

Faster edit loop: JS/HTML changes under `src/main/webapp` are published on save;
Java changes need `Ctrl+S` + hot swap or a server restart.

---

## 5. Project layout

```
seekora-backend/
├── pom.xml                         WAR packaging, Servlet API, MySQL, Hikari, Gson, jBCrypt
├── database/
│   ├── schema.sql                  full DDL
│   └── seed.sql                    demo/content data
└── src/main/
    ├── java/com/seekora/
    │   ├── config/DatabaseConfig    HikariCP pool, db.properties + env overrides
    │   ├── listener/                pool startup / shutdown
    │   ├── model/                   User, Plan, Subscription, KnowledgeEntry, …
    │   ├── dao/                     PreparedStatement-only JDBC access
    │   ├── service/                 validation + business rules
    │   ├── servlet/                 /api/* endpoints
    │   ├── filter/                  CORS, UTF-8 encoding, auth guard
    │   └── util/                    JSON, BCrypt, validation helpers
    ├── resources/db.properties
    └── webapp/
        ├── index.html               the page, wired to the API with fetch()
        ├── app/index.html           React application (created by the frontend package script)
        └── app/assets/              React production bundles
        └── WEB-INF/web.xml
```

Layering is strict: servlets never contain SQL, DAOs never contain business rules, and all SQL
goes through `PreparedStatement`, so the app is not exposed to SQL injection.

---

## 6. API reference

All responses use one envelope:

```json
{ "success": true, "message": "OK", "data": { } }
```

Errors return the same shape with `"success": false` and an HTTP status of 400/401/403/404/500.
Authentication is cookie/session based (`JSESSIONID`, HttpOnly).

### Auth
| Method | Path | Body |
|--------|------|------|
| POST | `/api/auth/signup` | `{fullName, email, password}` |
| POST | `/api/auth/login` | `{email, password}` |
| POST | `/api/auth/logout` | – |
| GET | `/api/auth/me` | – |

### Profile *(auth required)*
| Method | Path | Body |
|--------|------|------|
| GET | `/api/profile` | – |
| PUT | `/api/profile` | `{fullName}` |
| POST | `/api/profile/password` | `{currentPassword, newPassword}` |

### Search
| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/search` | `{query}` — works for guests too |
| GET | `/api/search?q=…` | same, convenience |
| GET | `/api/history?limit=20&offset=0` | auth required |
| DELETE | `/api/history/{id}` | auth required |
| DELETE | `/api/history` | clears history |

Freemium accounts are capped by `plans.daily_search_limit` (10/day); Premium uses `-1`
for unlimited. Guests are also limited to 10 searches per session per day. Each search uses
Gemini with Google Search grounding to answer the question and provide citations when Gemini returns
grounded source annotations. A matching saved knowledge topic may provide additional context.
Searches are stored in `search_queries` with the matched knowledge entry, response time, and source
count. `GEMINI_API_KEY` must be configured; the API does not present a saved demo answer as a live
web search when AI access is unavailable.

### Chat
| Method | Path | Body |
|--------|------|------|
| GET | `/api/chat` | conversation history |
| POST | `/api/chat` | `{message}` |

Replies come from the configured AI provider, which searches multiple relevant websites and
summarizes the findings with citations. Both sides of the conversation are persisted in
`chat_messages`; recent messages are included as follow-up context. Assistant replies are limited
to 20 per browser session per day.

### React AI conversations
| Method | Path | Body |
|--------|------|------|
| POST | `/api/ai` | `{query, mode, imageData?, conversationId?, history?, save?}` |
| GET | `/api/conversations` | auth required; current user's conversations |
| GET | `/api/conversations/{id}/messages` | auth required; conversation owner only |
| DELETE | `/api/conversations/{id}` | auth required; conversation owner only |

The React app sends AI requests through Tomcat; Gemini credentials remain server-side. General
question, chat, and research modes use Gemini with Google Search grounding and return citations
only when the provider supplies them. Saved requests require a signed-in user and store both
messages, mode, sources, and conversation metadata in MySQL. Requests with `save:false` do not
create a saved conversation.

### Webpage and image analysis
| Method | Path | Request |
|--------|------|---------|
| POST | `/api/analyze/url` | JSON `{ "url": "https://example.com/article" }` |
| POST | `/api/analyze/image` | multipart fields `image` (JPEG/PNG/GIF/WebP, up to 5 MB) and optional `prompt` |

URL analysis fetches public HTTP(S) HTML only, does not follow redirects, caps downloads at 1.5 MB,
and sends extracted page text to Gemini. Image analysis sends the selected image to Gemini; the
upload is not stored by Seekora. Image analysis is limited to 10 images per browser session. Both
features require `GEMINI_API_KEY`.

### Plans & subscriptions
| Method | Path | Body |
|--------|------|------|
| GET | `/api/plans` | – |
| GET | `/api/subscriptions` | auth required |
| POST | `/api/subscriptions` | `{planCode, billingCycle}` — `MONTHLY`/`YEARLY` |

Subscribing runs in a transaction: cancel the active subscription, insert the new one,
update `users.plan_id`.

### Content & misc
| Method | Path | |
|--------|------|--|
| GET | `/api/content/faqs` | verified demo FAQs |
| GET | `/api/content/testimonials` | empty until real, approved testimonials exist |
| GET | `/api/content/features` | verified demo feature descriptions |
| GET | `/api/content/stats` | live user/search/subscriber counters |
| GET | `/api/content/trending?limit=5` | most frequent recent queries |
| POST | `/api/newsletter` | `{email}` (idempotent) |
| DELETE | `/api/newsletter?email=…` | |
| POST | `/api/contact` | `{name, email, subject, message}` |
| GET | `/api/health` | verifies the MySQL connection |

### Example

```bash
curl -c cookies.txt -X POST http://localhost:8080/seekora/api/auth/signup \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Your Name","email":"you@example.com","password":"choose-a-password"}'

curl -b cookies.txt -X POST http://localhost:8080/seekora/api/search \
  -H 'Content-Type: application/json' -d '{"query":"quantum computing"}'
```

---

## 7. Frontend wiring

`index.html` is the original landing page and uses the Java backend for search and account features.
The React frontend is packaged separately under `/app/` by running
`npm run build:tomcat -- -BackendPath 'C:\path\to\seekora-backend'` from the frontend project:

- The React app uses `/api/auth/*` and Tomcat's session cookie for MySQL-backed authentication.
- React AI questions use `/api/ai`; saved threads and messages use `/api/conversations`.
- Newsletter, contact messages, and demo subscriptions use `/api/newsletter`, `/api/contact`, and
  `/api/subscriptions`, backed by MySQL.
- No Supabase client or credentials are required. The server requires `GEMINI_API_KEY` to generate
  AI answers and never exposes that key to browser code.

- `doAuth()` → `/api/auth/signup` or `/api/auth/login`, then updates the navbar button to
  "Hi, <name> · Log out".
- demoSearch() calls /api/search, renders matching local topics and stored source links, and reports unknown topics without inventing answers.
- `sendChat()` → `/api/chat`.
- `subscribe()` → `/api/newsletter`.
- `choosePlan('PREMIUM')` → `/api/subscriptions`, respecting the monthly/yearly toggle.
- Plan selection is a demonstration flow and does not process a payment.
- On load the page restores the session (`/api/auth/me`) and reads live user/search/subscriber totals from MySQL. FAQ and feature API responses use verified demo copy; legacy marketing rows are not exposed.

---

### Request flow (MVC layers)

This deployment uses the existing Servlet MVC rather than Spring MVC: the browser sends JSON or
multipart data to a servlet controller; the servlet validates and decodes the request, then calls a
service; the service applies business rules, calls the AI provider when configured, and/or calls a
DAO; DAOs run parameterized JDBC statements through the Hikari connection pool; the servlet returns
the model/result in the shared JSON envelope. The landing page and React application are static
frontends served by Tomcat; neither requires JSP.

## 8. Notes before production

- Change the MySQL password and supply it through the `SEEKORA_*` environment variables.
- Serve over HTTPS and add `Secure` to the session cookie in `web.xml`.
- Keep database credentials in environment variables, not source files.
- Cross-origin access is off unless exact allowed origins are configured.
- General answers require a Gemini API key configured in the Tomcat environment. Gemini API and
  Google Search grounding usage may be billed; apply account spending limits appropriate for your
  deployment.
- URL analysis rejects private/reserved addresses and redirects. For production with untrusted URL
  input, use a network egress proxy that enforces destination IP policy at connection time.
- Search history saves the answer and source count, but source metadata is not yet retained per
  history row. Image analyses are returned to the browser and are not persisted.
- There are no repository test sources. `mvn test package` verifies compilation and WAR packaging;
  database, browser, and live provider workflows still need configured services for end-to-end checks.
