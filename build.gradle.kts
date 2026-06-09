plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
    id("run-hytale")
}

group = "com.Ev0sMods"
version = "1.0.0"
description = "Ev0's EZ Grow - Crouch to instantly advance nearby crops and saplings!"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "hytale"
        url = uri("https://maven.hytale.com/release")
    }
    maven {
        name = "hytale-pre-release"
        url = uri("https://maven.hytale.com/pre-release")
    }
}

dependencies {
    val hytaleBuild = findProperty("hytale_build") as String? ?: "+"
    compileOnly("com.hypixel.hytale:Server:$hytaleBuild")

    val hytaleHome = System.getProperty("user.home") + "/AppData/Roaming/Hytale"
    val patchline = "release"
    val localJar = file("$hytaleHome/install/$patchline/package/game/latest/Server/HytaleServer.jar")
    if (localJar.exists()) {
        compileOnly(files(localJar))
    }

    implementation("org.jetbrains:annotations:24.1.0")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        filesMatching("manifest.json") {
            expand(mapOf(
                "group" to project.group,
                "version" to project.version,
                "description" to project.description
            ))
        }
    }

    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
