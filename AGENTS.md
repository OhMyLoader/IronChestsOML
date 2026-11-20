# AGENTS.md — IronChestsOML

Read [CONTRIBUTING.md](CONTRIBUTING.md) first. This file adds only constraints that matter when an
agent works in this repository.

- This repository is a mod project consuming OhMyLoader through Maven coordinates: the plugin and
  every `org.ohmyloader:*` artifact resolve from Maven (mavenLocal during development). Do not add
  sibling source paths, shared catalogs, `includeBuild`, or source mounts.
- Schema and API changes start in the loader repositories; keep this repository's usage in lockstep.
- Run `./gradlew build test` for code changes; a behavior change needs a real game run in
  OhMyLoaderTestMod, not just unit tests.
- Commit in this repository. Do not push, tag, or publish unless explicitly asked.
