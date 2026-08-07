# POS Nature CRUD

Java 8 Swing prototype with a project-local MariaDB development database. Nothing here installs software globally or touches Docker resources outside the fixed `pos-nature-crud` project.

## Local workflow

```bash
./scripts/pos setup       # downloads and verifies a local Temurin Java 8 runtime
./scripts/pos build       # builds only in the pinned Maven Java 8 container
./scripts/pos db-up       # starts MariaDB at 127.0.0.1:3307
./scripts/pos smoke       # runs JDBC SELECT 1
./scripts/pos run         # starts the Swing application through local Java 8
./scripts/pos package     # writes JAR, checksum, and notes under dist/
./scripts/pos db-down     # stops only this project's database
./scripts/pos purge --yes # removes only this project's DB volume and local generated files
```

`setup` creates ignored `.env` from `.env.example`. Keep credentials in `.env`; do not commit it. `db-up` refuses to start when port 3307 is occupied. `purge` intentionally does not remove Docker images or run any Docker-wide prune command.

## Database smoke check

The application calls `DbSmoke.run()` for `--db-smoke`. It reads `POS_DB_URL`, `POS_DB_USER`, and `POS_DB_PASSWORD`, opens JDBC with a five-second login timeout, performs only `SELECT 1`, and closes every JDBC resource.

## Release boundary

`package` produces local contents in `dist/`. A release is ready only after a local commit and tag are made by the coordinator. This repository never pushes or publishes from the launcher.

