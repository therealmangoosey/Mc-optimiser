plugins {
    id("java")
}

group = "com.mc.optimizer"
version = "1.2.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Dynamic range picks up the latest 26.2 build. Pin to an exact build
    // string (e.g. "26.2.build.119-stable") instead if you want reproducible
    // builds - check https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/
    // for available builds.
    compileOnly("io.papermc.paper:paper-api:[26.2.build,)")
    // paper-api declares org.jetbrains:annotations as compileOnly and does not
    // inject it into its published POM, so consumers must declare it themselves.
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
}
