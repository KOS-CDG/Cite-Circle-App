# Room exported schemas — ACTION REQUIRED once

This directory is where Room writes its exported JSON schema (configured by
`ksp { arg("room.schemaLocation", ...) }` in `app/build.gradle.kts`).

**It is currently empty, and one file needs to be generated and committed before the database
schema is changed.**

## Why this matters

`AppDatabase` is at `version = 1` with `exportSchema = true`. Room can only generate an
`@AutoMigration(from = 1, to = 2)` if the exported JSON for **both** endpoints exists on disk.

This project originally shipped with `exportSchema = false`, so no `1.json` was ever written —
and it **cannot be produced retroactively** once the version moves past 1. Capturing it now is
the difference between the next schema change being a one-line annotation and being hand-written
SQL that must byte-match Room's internal schema hash, with a runtime crash as the only feedback.

## What to do

While `AppDatabase` is still at `version = 1`, run a build on a machine with the Android SDK:

```sh
./gradlew :app:kspDebugKotlin      # or simply: ./gradlew assembleDebug
```

Then commit the generated file:

```sh
git add app/schemas/com.example.data.AppDatabase/1.json
git commit -m "chore(room): capture v1 schema before any migration work"
```

Once `1.json` is committed, this README can be deleted.

## Do not bump the database version before doing this

`MyApplication` sets `fallbackToDestructiveMigration(dropAllTables = true)`, so a version bump
without a migration will not crash — but it *will* wipe the local database. That is acceptable
for the seeded sample data this app currently holds, and it is not a substitute for a real
migration path.
