import net.minecrell.pluginyml.paper.PaperPluginDescription
import java.util.*
import java.text.SimpleDateFormat

plugins {
    id("java")
    id("maven-publish")
    id("net.kyori.blossom") version "2.2.0"
    id("com.gradleup.shadow") version ("9.3.1")
    id("de.eldoria.plugin-yml.paper") version ("0.8.0")
}

group = "eu.koolfreedom"
version = "1.0"
description = "KoolChatFilter"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

paper {
    name = rootProject.name.toString()
    version = project.version.toString()
    description = "Random chat filter plugin"
    main = "eu.koolfreedom.KoolChatFilter"
    loader = "eu.koolfreedom.KoolLibraryManager"
    website = "https://github.com/KoolFreedom"
    authors = listOf("gamingto12")
    apiVersion = "1.21.10"
    generateLibrariesJson = true
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("org.reflections:reflections:0.10.2")

    // Misc
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks {
    // Update build.properties with build information
    processResources {
        val buildAuthor = project.findProperty("buildAuthor")?.toString() ?: "KoolFreedom"
        val buildNumber = getBuildNumber()
        val buildDate = SimpleDateFormat("M/dd/yyyy 'at' h:mm:ss aa zzz").format(Date())

        inputs.properties(
            mapOf(
                "buildAuthor" to buildAuthor,
                "buildNumber" to buildNumber,
                "buildVersion" to project.version,
                "buildDate" to buildDate
            )
        )

        filesMatching("build.properties") {
            expand(
                "buildAuthor" to buildAuthor,
                "buildNumber" to buildNumber,
                "buildVersion" to project.version,
                "buildDate" to buildDate
            )
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    shadowJar {
        archiveFileName.set("KoolChatFilter-${project.version}.jar")

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/versions/**")

        mergeServiceFiles()
        relocate("com.google.gson", "eu.koolfreedom.libs.gson")
    }

    build {
        dependsOn(shadowJar)
    }
}

fun getBuildNumber(): Int {
    val current = (project.findProperty("buildNumber")?.toString()?.toIntOrNull() ?: 0)
    project.extensions.extraProperties["buildNumber"] = current + 1
    return current + 1
}