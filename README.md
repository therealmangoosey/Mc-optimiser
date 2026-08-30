# MCOptimizer

MCOptimizer is a server-side Paper optimization plugin for Minecraft 26.2.

## Requirements

- Paper 26.2
- Java 25

Spigot-compatible metadata is retained, but Paper 26.2 is the supported target.

## Installation

1. Download the `MCOptimizer-<version>.jar` release file.
2. Put the JAR in your server's `plugins/` directory.
3. Restart the server.
4. Configure `plugins/MCOptimizer/config.yml` if needed.

Do not upload the source ZIP or build ZIP as the Modrinth primary file. The plugin JAR is the file intended for server installation.

## Building

The repository includes the Gradle wrapper.

```bash
./gradlew clean build
```

The release JAR is generated as:

```text
build/libs/MCOptimizer-<version>.jar
```

The version is defined by the `# Version:` line in `src/main/resources/config.yml` and is injected into `plugin.yml` by Gradle.

## Compatibility

MCOptimizer targets Paper 26.2 and Java 25. The project uses legacy `plugin.yml` metadata for the plugin descriptor.

## Configuration and safety

MCOptimizer exposes several optimization systems through `config.yml`. Features that can change gameplay or have limited benefit on modern Paper are kept conservative or disabled by default rather than silently changing server behaviour.

Always test configuration changes on a backup or staging server before using them on a production world.

## Releases

GitHub Actions builds the plugin on pushes to `main`, pull requests, and manual runs. Release builds publish the versioned plugin JAR and source ZIP to GitHub Releases.

For Modrinth, upload the versioned plugin JAR as the primary file and set the version's Minecraft compatibility to 26.2.
