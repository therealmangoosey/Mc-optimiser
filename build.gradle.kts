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
    // Pinned to an exact build for reproducible CI builds.
    //
    // NOTE: do NOT go back to the dynamic range "[26.2.build,)". Gradle splits
    // versions on [. - _ +] and on letter/digit boundaries, and treats the
    // string parts "rc", "release" and "final" as HIGHER than any other string
    // part. So "26.2-rc-2.build.9-alpha" -> [26,2,rc,2,...] sorts ABOVE
    // "26.2.build.121-stable" -> [26,2,build,121,...] because "rc" > "build".
    // The range would therefore resolve to an old release-candidate alpha, and
    // being open-ended it would also drift onto 26.3/27.x once those publish.
    //
    // Check https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/
    // for newer builds and bump this deliberately.
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
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
