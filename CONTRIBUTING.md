# Contributing to IronChestsOML

A mod project for OhMyLoader, built with the `org.ohmyloader.gradle` plugin exactly the way a
third-party mod project builds: everything resolves from Maven, so a broken publish surface fails
here and not on release day.

## Prerequisites

- JDK 27 (Gradle provisions the toolchain).
- The Gradle wrapper in this repository.
- A `publishToMavenLocal` run in the loader (and the plugin) checkout when developing against a
  local loader change.

## Build and check

```bash
./gradlew build test
```

## Commit and pull request

- Conventional Commits (`feat: ...`, `fix: ...`). One concern per commit.
- Branch from `main`; pull requests only. CI gates every pull request.
