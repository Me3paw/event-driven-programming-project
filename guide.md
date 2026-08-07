# Plain Eclipse Java 8 project

This project uses no Maven or Gradle. Open the folder with **File > Import > Existing Projects into Workspace**, then configure Eclipse's installed JRE as Java 8. The committed `.classpath` uses `src`, `JavaSE-1.8`, and the manually managed `lib/mariadb-java-client-3.5.7.jar`.

Run `graphicUI.PosApplication` as a Java Application. Pass `--db-smoke` in the run configuration to test the database connection without changing data.

Set `POS_DB_URL`, `POS_DB_USER`, and `POS_DB_PASSWORD` before calling `connectDB.DBConnection.smoke()`. It performs only `SELECT 1`: `0` success, `2` missing environment value, `3` unexpected result, `4` JDBC failure.

`sql/Script.sql` is intentionally empty of schema: add tables only after requirements are known.
