# Plain Eclipse Java 8 POS Plan

## Goal

Build a simple Java 8 Swing POS prototype as an Eclipse Java Project with manual dependency management. The repository must remain easy to import and resemble the requested structure:

```text
.
├── .settings/
├── lib/
│   └── mariadb-java-client-3.5.7.jar
├── sql/
│   └── Script.sql
├── src/
│   ├── components/
│   ├── connectDB/
│   ├── dao/
│   ├── entity/
│   └── graphicUI/
├── .classpath
├── .gitignore
├── .project
├── guide.md
└── PLAN.md
```

No Maven, Gradle, build wrapper, dependency resolver, ORM, UI framework, or generated domain design.

## Scope

- Java 8 and native Swing only.
- Dashboard, Products, Customers, and Sales prototype screens.
- Sample in-memory rows; UI actions must not claim to persist data.
- MariaDB Connector/J stored directly under `lib/` and referenced by `.classpath`.
- One read-only database smoke check using `SELECT 1`.
- Schema, entities, and DAOs remain deferred until the assignment requirements are supplied.
- Git changes stay local; do not push or publish from this machine.

## Completed

- [x] Audited the previous Maven/container scaffold with three `gpt-5.6-terra` medium-effort agents.
- [x] Removed Maven, Docker Compose, launcher, generated release, and old package-layout files.
- [x] Added Eclipse `.project`, `.classpath`, and Java 8 compiler settings.
- [x] Set the Eclipse project resource encoding to UTF-8.
- [x] Added the manually managed MariaDB Connector/J `3.5.7` JAR under `lib/`.
- [x] Moved the Swing entry point and frame to `src/graphicUI/`.
- [x] Added `src/connectDB/DBConnection.java` with environment-based credentials and `SELECT 1` smoke check.
- [x] Kept `components`, `dao`, and `entity` visible using `package-info.java` without inventing unused classes.
- [x] Added schema-free `sql/Script.sql` and Eclipse usage instructions in `guide.md`.
- [x] Compiled with Temurin Java 8 and confirmed class major version `52`.
- [x] Ran the JDBC smoke check successfully against the existing local MariaDB instance.
- [x] Ran an automated Java 8/Xvfb Swing smoke test covering initial state, all navigation cards, status updates, sample action, focus restoration, minimum size, and close behavior.
- [x] Confirmed automated checks left no compiled classes, caches, temporary harness files, or credentials in the repository.
- [x] Passed XML parsing, JAR integrity, `git diff --check`, UI audit, and final read-only swarm review with no blockers.

## Remaining

- [ ] Import the project in Eclipse using **File > Import > Existing Projects into Workspace**.
- [ ] Select an installed Java 8 JDK for the `JavaSE-1.8` execution environment.
- [ ] Run `graphicUI.PosApplication` and manually verify navigation, forms, tables, resizing, keyboard focus, and window close behavior.
- [ ] Add the real SQL schema, entity classes, and DAO classes only after the assignment rubric is available.
- [ ] Review the final Git diff with the user.
- [ ] Commit locally and create the local `v0.1.0` tag only after approval.
- [ ] Keep the repository unpushed and unpublished until the user performs those actions.

## Current State

- Branch: `main`
- Remote: `git@github.com:Me3paw/event-driven-programming-project.git`
- Latest existing commit: `14d4b1e feat: scaffold Java 8 Swing POS`
- Refactor: implemented and validated, not committed
- Tag: not created
- Push/release: not performed

## Boundaries

- Do not add Maven or Gradle.
- Do not guess the assignment schema or create placeholder CRUD implementations.
- Do not commit credentials; database values come from `POS_DB_URL`, `POS_DB_USER`, and `POS_DB_PASSWORD`.
- Do not delete or modify the existing MariaDB container or volume during source refactoring.
- Do not push, publish, install global packages, or alter the system Java configuration.
