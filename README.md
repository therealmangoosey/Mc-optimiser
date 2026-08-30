# MCOptimizer

MCOptimizer is a server-side Minecraft optimization plugin designed to run on Bukkit-compatible servers, including Bukkit, Spigot, and Paper.

## Requirements

- Minecraft 26.2
- Java 25
- A Bukkit-compatible server implementation: Bukkit, Spigot, or Paper

The same plugin JAR is built against the Spigot API, which contains the Bukkit API. No Paper-only API is required by the plugin source.

## Installation

1. Download the `MCOptimizer-<version>.jar` release file.
2. Put the JAR in your server's `plugins/` directory.
3. Restart the server.
4. Configure `plugins/MCOptimizer/config.yml` if needed.

Do not upload the source ZIP or build ZIP as the primary plugin file. The plugin JAR is the file intended for server installation.

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

MCOptimizer is compiled against the Spigot API for Minecraft 26.2. Because the source uses Bukkit/Spigot API rather than Paper-only APIs, the same JAR is intended for:

- Bukkit 26.2-compatible servers
- Spigot 26.2
- Paper 26.2

Paper 26.2 exposes the Bukkit and Spigot API packages as part of its plugin API, while Spigot's API documentation is the compatibility baseline used by this project. The project does not compile against Paper-only classes.

## Configuration and safety

MCOptimizer exposes several optimization systems through `config.yml`. Features that can change gameplay or have limited benefit on modern server software are kept conservative or disabled by default rather than silently changing server behaviour.

Always test configuration changes on a backup or staging server before using them on a production world.

## Startup update check

Every server start checks the latest GitHub Release asynchronously. The plugin reports whether it is up to date or behind, including the version difference, without automatically replacing the server JAR.

## Releases

GitHub Actions builds the plugin on pushes to `main`, pull requests, and manual runs. Release builds publish the versioned plugin JAR and source ZIP to GitHub Releases.

For Modrinth, upload the versioned plugin JAR as the primary file and set the version's Minecraft compatibility to 26.2.
