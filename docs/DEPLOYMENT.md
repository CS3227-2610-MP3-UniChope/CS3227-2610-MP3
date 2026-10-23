# Deploy to Render

The root Dockerfile builds React with Node24, bundles it into Spring Boot, and
runs the resulting Java25 application as a non-root user. One Render web service
serves both the UI and /api. PostgreSQL remains a separate managed database.

## Database

Create a PostgreSQL17 database on Render and choose the same region as the web
service. Use its internal host, database name, username and password for the app.
Render's free database expires after30days; use a paid database or another managed
PostgreSQL provider if it must remain available longer.

## Web service

1. Connect the GitHub repository in Render and create a Web Service.
2. Choose main, Docker runtime, repository root as the build context, and
   ./Dockerfile as the Dockerfile path. Leave the Docker command override empty.
3. Choose the database's region and your instance size.
4. Configure the following environment variables before deploying:

| Variable | Value |
| --- | --- |
| UNICHOPE_JDBC_URL | jdbc:postgresql://INTERNAL_HOST:5432/DATABASE_NAME |
| UNICHOPE_JDBC_USER | Database username |
| UNICHOPE_JDBC_PASSWORD | Database password |
| UNICHOPE_AI_MODE | shared for real AI; unavailable to disable it |
| SOC_LLM_API_KEY_ADMIN | Private Admin provider key |
| SOC_LLM_API_KEY_STUDENT | Private Student provider key |
| SOC_LLM_API_KEY_TUTOR | Private Tutor provider key |

The image sets app-api, listens on 0.0.0.0 and uses Render's PORT (default10000).
It enables framework forwarded-header handling for Render's TLS proxy. Secure,
HttpOnly session cookies remain enabled. Do not add local/local-soc profiles or
bake database credentials or provider keys into the image. .env is not loaded
automatically; configure actual runtime variables in Render.

Leave the optional health-check path unset for the initial setup; there is no
Actuator health endpoint. Deploy and wait for the application-started log message.
Existing Flyway migrations run on startup. The public service URL serves both the
frontend and APIs; no separate Vite server or CORS configuration is required.

## Initial Admin

Use the existing offline bootstrapAdmin Gradle task from your local machine with
Java25. Set UNICHOPE_JDBC_URL to the database's external JDBC connection address
(with sslmode=require for a TLS connection), and set its username/password plus
UNICHOPE_ADMIN_NAME, UNICHOPE_ADMIN_EMAIL and UNICHOPE_ADMIN_PASSWORD privately.
From src/backend run:

```powershell
.\gradlew.bat bootstrapAdmin
```

Remove the bootstrap credential variables afterwards. Do not configure automatic
sample seeding or a public setup endpoint. Existing local accounts do not appear
in the hosted database automatically.

## Check the deployment

Open /login, sign in, and refresh a nested route such as /admin/modules or
/student/slots. Verify an assigned role cannot access another role's API, signing
out ends the session, and AI failures show errors without switching to fake output.
The /api paths must return their API responses, never index.html.

Free web services can sleep after15idleminutes and take about a minute to wake.
Sessions are in memory: restarting or sleeping the backend requires signing in
again. Database records persist independently of the application container. Keep
the service at one running instance while sessions remain in memory.

## Local image check

From the repository root with Docker running:

```powershell
docker build -t unichope .
docker run --rm -p 10000:10000 --env-file PATH_TO_PRIVATE_ENV_FILE unichope
```

The private environment file must use a PostgreSQL host reachable from inside the
container (localhost means the container itself). When using a host database on
Docker Desktop, configure host.docker.internal and allow that local development
connection. Do not commit the file. The image's default profiles remain secure;
use HTTPS for the real sign-in flow, or test local HTTP with explicitly local
runtime profiles only.

Render references: [Docker](https://render.com/docs/docker),
[web services](https://render.com/docs/web-services),
[environment settings](https://render.com/docs/configure-environment-variables),
[free limits](https://render.com/docs/free).
