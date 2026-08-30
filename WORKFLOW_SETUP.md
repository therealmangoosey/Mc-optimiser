# CI workflow: manual step required

The CI workflow is committed to this repo at **`ci/build-and-release.yml`**,
not at its real path. The Arena bot token lacks the GitHub App `workflows`
permission, so any push containing a file under `.github/workflows/` is
rejected by GitHub:

    refusing to allow a GitHub App to create or update workflow
    `.github/workflows/build-and-release.yml` without `workflows` permission

This is a hard server-side restriction; it cannot be worked around from here.

## How to add it

The workflow is already committed at `ci/build-and-release.yml`. It only needs
to be moved into place, which requires credentials with the `workflow` scope
(i.e. you, not the bot):

    git fetch origin
    git checkout arena/01a04fff-mc-optimiser
    git pull
    mkdir -p .github/workflows
    git mv ci/build-and-release.yml .github/workflows/build-and-release.yml
    git commit -m "Add JAR build and release workflow"
    git push origin arena/01a04fff-mc-optimiser

No copy-paste is needed - the exact validated file is already in the repo.

Then merge to `main`. **The workflow only becomes active once it is on the
default branch**, and only a push to `main` produces the rolling `latest`
release with the JAR attached. Merging PR #2 will do both at once.

## Before the first run: regenerate the Gradle wrapper

`gradle/wrapper/gradle-wrapper.jar` has SHA-256

    2b2e2cee3d8a8e5379b4f1c5902419404e83c1dba5ff55192ad5986e3f44cd6e

which matches **no released Gradle version** — it is the wrapper JAR from an
unreleased 9.8.0 milestone, while `gradle-wrapper.properties` claimed 8.12.
Because of this the workflow currently runs a downloaded Gradle 9.1.0 with
`validate-wrappers: false` instead of trusting `./gradlew`.

Fix it properly with:

    gradle wrapper --gradle-version 9.1.0

then in the workflow drop the `gradle-version` / `validate-wrappers` inputs
and change the build step to `./gradlew --stacktrace build`.

## What the workflow does

| Trigger              | JAR built | Artifact | Release                          |
|----------------------|-----------|----------|----------------------------------|
| push to `main`       | yes       | yes      | rolling `latest` (prerelease)    |
| push tag `v*`        | yes       | yes      | versioned release, marked Latest |
| pull request         | yes       | yes      | none                             |
| `workflow_dispatch`  | yes       | yes      | none                             |

The rolling release is published as a **prerelease** so it never steals the
"Latest" badge from a real tagged version, and is deleted/recreated (with
`--cleanup-tag`) so its tag follows `main`.

## Untested

No Actions run has happened yet, and this sandbox has no JDK and no network
access to `repo.papermc.io`, `services.gradle.org` or Maven Central, so the
Gradle build has never actually been executed. The 26 decompiled sources are
still only statically audited. Expect the first run to surface real compile
errors.
