import java.util.regex.Pattern

plugins {
    id("java")
}

val configFile = file("src/main/resources/config.yml")
val versionPattern = Pattern.compile("(?m)^# Version: ([0-9]+\\.[0-9]+\\.[0-9]+)\\s*$")
val configVersion = versionPattern.matcher(configFile.readText()).let { matcher ->
    require(matcher.find()) { "Could not find '# Version: x.y.z' in ${configFile.path}" }
    matcher.group(1)
}

group = "com.mc.optimizer"
version = configVersion

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    // Compile against the Spigot API, which contains the Bukkit API.
    // Avoid Paper-only APIs so the same JAR targets Bukkit/Spigot and Paper.
    compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesMatching("plugin.yml") {
        expand("version" to project.version.toString())
    }
}
