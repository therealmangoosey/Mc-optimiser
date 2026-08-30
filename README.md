# MCOptimizer

MCOptimizer is a Paper/Spigot server optimization plugin targeting Minecraft 26.2 and Java 25.

## Build

The repository includes the Gradle wrapper. Run:

```bash
./gradlew clean build
```

The plugin JAR is written to:

```text
build/libs/
```

## GitHub Actions

Every push to `main`, pull request, or manual workflow run builds the plugin with Java 25 and uploads the resulting JAR as the `MCOptimizer-JAR` workflow artifact.

Open the repository's **Actions** tab, select **Build MCOptimizer**, then download the artifact from a successful run.

## Install

Copy the generated JAR into the server's `plugins/` directory and restart the Paper/Spigot server.

## Compatibility

- Minecraft / Paper API: 26.2
- Java: 25
- Build system: Gradle 8.12

The project uses legacy `plugin.yml` metadata for broad Paper/Spigot compatibility. The removed `paper-plugin.yml` contained invalid dependency metadata for the current Paper plugin format.
