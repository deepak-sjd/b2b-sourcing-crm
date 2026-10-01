# VS Code Setup Guide

## Your situation: Java 21 and Java 24 both already installed

You don't need to install Java. You just need this project to **use 21**
specifically, without disturbing whatever other project relies on 24. Two
places control this, and both need to point at 21:

1. **VS Code's Java language server** → controlled by `.vscode/settings.json` → `java.configuration.runtimes` (already set up in this repo — you just need to fix the paths, see below)
2. **The `mvn` command you run in a terminal** → controlled by the `JAVA_HOME` environment variable *for that terminal session* — this is separate from VS Code's language server setting

### Fix the JDK paths (one-time)

Open PowerShell and run:

```powershell
Get-ChildItem "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Java" -ErrorAction SilentlyContinue
```

Find your actual Java 21 and Java 24 folder names, then open
`.vscode/settings.json` in this project and replace the two placeholder paths
(`jdk-21.0.4+7-hotspot` and `jdk-24.0.1+9-hotspot`) with your real folder
names. This affects **only this project** — nothing system-wide changes.

### Set JAVA_HOME for terminal commands (every new terminal, until you automate it)

Before running any `mvn` command in a plain PowerShell terminal, run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.4+7-hotspot"
java -version   # should print 21.x
```

**To avoid doing this every time**, add a `.vscode/settings.json` entry
(already done above via `maven.terminal.customEnv`) — VS Code's *integrated*
terminal will auto-set `JAVA_HOME` to 21 whenever you open it from inside
this project folder. Terminals opened outside VS Code still need the manual
`$env:JAVA_HOME` line.

## 1. Install what's actually missing

| Tool | Do you need it? | Check with |
|---|---|---|
| JDK 21 | ✅ Already have it | `& "C:\Program Files\Eclipse Adoptium\jdk-21.0.4+7-hotspot\bin\java.exe" -version` |
| **Maven** | Check — you likely don't have this yet | `mvn -v` |
| Docker Desktop | Check | `docker --version` |
| VS Code | Check | — |

**Install Maven** if `mvn -v` fails:
```powershell
winget install Apache.Maven
```
Then close and reopen your terminal, and re-run `mvn -v` to confirm.

**Install Docker Desktop** if `docker --version` fails:
```powershell
winget install Docker.DockerDesktop
```
Docker Desktop needs WSL2 — if prompted, let it install/enable WSL2, then
restart your machine once.


## 2. Open the project

```bash
cd doctutorials-crm
code .
```

VS Code will prompt: **"This workspace has extension recommendations"** →
click **Install All**. This installs the Java Extension Pack, Spring Boot
Dashboard, Lombok support, and a Postgres client — everything configured in
`.vscode/extensions.json`.

## 3. (Optional) Generate the Maven Wrapper

Not required — you now have Maven installed system-wide, so plain `mvn`
commands work fine. If you'd rather pin an exact Maven version for this repo
(recommended once you start collaborating with others):

```bash
mvn -N wrapper:wrapper -Dmaven=3.9.9
```

This creates `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/`. If you generate it,
use `./mvnw` instead of `mvn` in every command below and in `.vscode/tasks.json`.

## 4. Set up your environment file

```bash
cp .env.example .env.local
```

Edit `.env.local` if you want different DB credentials — the defaults match
`docker-compose.yml` so you can leave them as-is for local dev.

## 5. Start Postgres

Easiest: use the VS Code task (`Ctrl+Shift+P` → "Run Task" → **"docker: start
postgres only"**), or from the terminal:

```bash
docker compose up -d postgres
```

Verify it's actually up before moving on:
```bash
docker ps    # should show crm-postgres as "healthy"
```

## 6. Verify it compiles

Open a terminal **inside VS Code** (so `JAVA_HOME` is auto-set per the
settings above), then:

```bash
mvn compile
```

If you're in an external terminal instead, set `JAVA_HOME` to your JDK 21
path first (see the top of this doc).

**If this fails, copy the exact error and send it to me — don't try to debug
Spring/Hibernate errors alone, that's exactly what I'm here for.**

## 7. Run the tests

```bash
mvn test
```

These run against an in-memory H2 database — no Postgres needed for this step.

## 8. Run/debug the app

Press **F5** in VS Code, or use the Spring Boot Dashboard (elephant icon in
the sidebar) → click the ▶ next to `crm`. This uses the launch config already
set up in `.vscode/launch.json` (which forces JDK 21 via the runtime config,
regardless of what `JAVA_HOME` happens to be set to elsewhere).

Once running: open http://localhost:8080/swagger-ui.html to see every
endpoint and try requests directly from the browser.

## 9. Everyday workflow from here

- `Ctrl+Shift+P` → "Run Task" → **"mvn: compile"** or **"mvn: test"** any time
- The Spring Boot Dashboard shows live app status, hot-reload on save (via `spring-boot-devtools` — add it if you want this: `<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-devtools</artifactId><scope>runtime</scope><optional>true</optional></dependency>`)
- SQLTools (Postgres extension) lets you browse the actual database tables from the sidebar — connect to `localhost:5432`, db `doctutorials_crm`, user `crm_user`

## 10. When you're ready for the next feature

Check `MVP_SCOPE.md` for what's next, tell me which module, and we build it
the same way: entity → repository → service → controller → migration → test
→ you verify it runs.
