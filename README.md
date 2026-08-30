# MCOptimizer

MCOptimizer is a Paper/Spigot server optimization plugin targeting Minecraft 26.2 and Java 25.

## Version

The version is defined in `src/main/resources/config.yml` on the `# Version:` line. Gradle reads that value automatically, injects it into `plugin.yml`, names the JAR/ZIP with it, and uses it for the GitHub Release tag.

## Build

The repository includes the Gradle wrapper. Run:

```bash
./gradlew clean build
```

The plugin JAR is written to:

```text
build/libs/MCOptimizer-<version>.jar
```

## GitHub Actions

Every push to `main`, pull request, or manual workflow run builds the plugin with Java 25. Successful pushes to `main` also publish a GitHub Release using the version from `config.yml` and attach:

```text
MCOptimizer-<version>.jar
MCOptimizer-<version>.zip
```

Pull requests build and validate the project but do not publish releases.

## Install

Copy the generated JAR into the server's `plugins/` directory and restart the Paper/Spigot server.

## Compatibility

- Minecraft / Paper API: 26.2
- Java: 25
- Build system: Gradle 9.1.0

The project uses legacy `plugin.yml` metadata for broad Paper/Spigot compatibility. The removed `paper-plugin.yml` contained invalid dependency metadata for the current Paper plugin format.
