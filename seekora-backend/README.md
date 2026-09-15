# Seekora — AI Information Platform (Java Servlet + JDBC + MySQL)

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

`schema.sql` drops and recreates the `seekora_db` database with all tables, indexes,
foreign keys, a full-text index on the knowledge base and the `v_user_searches_today` view.
`seed.sql` loads plans, plan features, demo users, knowledge topics + sources, chat intents,
features, FAQs, testimonials and platform stats.

### Demo accounts

| Email | Password | Plan |
|-------|----------|------|
| `admin@seekora.com` | `Passw0rd!` | Premium (ADMIN) |
| `demo@seekora.com` | `Passw0rd!` | Freemium |

### Connection settings

`src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/seekora_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.user=root
db.password=root
```

Every value can be overridden with environment variables, which is what you should do in
production instead of committing a password:

```
SEEKORA_DB_URL, SEEKORA_DB_USER, SEEKORA_DB_PASSWORD
```

---

## 3. Build and run from the command line

```bash
mvn clean package                       # produces target/seekora.war
cp target/seekora.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

Open http://localhost:8080/seekora/

Health check: http://localhost:8080/seekora/api/health

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
for unlimited. Every query is stored in `search_queries` with the matched knowledge entry,
accuracy, response time and source count.

### Chat
| Method | Path | Body |
|--------|------|------|
| GET | `/api/chat` | conversation history |
| POST | `/api/chat` | `{message}` |

Replies come from the `chat_intents` table (keyword matched), and both sides of the
conversation are persisted in `chat_messages`.

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
| GET | `/api/content/faqs` | |
| GET | `/api/content/testimonials` | |
| GET | `/api/content/features` | |
| GET | `/api/content/stats` | live user/search/subscriber counters |
| GET | `/api/content/trending?limit=5` | most frequent recent queries |
| POST | `/api/newsletter` | `{email}` (idempotent) |
| DELETE | `/api/newsletter?email=…` | |
| POST | `/api/contact` | `{name, email, subject, message}` |
| GET | `/api/health` | verifies the MySQL connection |

### Example

```bash
curl -c cookies.txt -X POST http://localhost:8080/seekora/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@seekora.com","password":"Passw0rd!"}'

curl -b cookies.txt -X POST http://localhost:8080/seekora/api/search \
  -H 'Content-Type: application/json' -d '{"query":"quantum computing"}'
```

---

## 7. Frontend wiring

`index.html` keeps its original design; the mock data was replaced by real calls:

- `doAuth()` → `/api/auth/signup` or `/api/auth/login`, then updates the navbar button to
  "Hi, <name> · Log out".
- `demoSearch()` → `/api/search`, renders the answer, the source links, the accuracy score,
  the response time, and warns when free searches run low.
- `sendChat()` → `/api/chat`.
- `subscribe()` → `/api/newsletter`.
- `choosePlan('PREMIUM')` → `/api/subscriptions`, respecting the monthly/yearly toggle.
- On load the page restores the session (`/api/auth/me`) and pulls stats and FAQs from MySQL.

---

## 8. Notes before production

- Change the MySQL password and supply it through the `SEEKORA_*` environment variables.
- Serve over HTTPS and add `Secure` to the session cookie in `web.xml`.
- `CorsFilter` currently echoes the request origin; restrict it to your own domain.
