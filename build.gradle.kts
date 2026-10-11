import java.util.*
import java.text.SimpleDateFormat

plugins {
    id("java")
    id("net.kyori.blossom") version "2.2.0"
    id("com.gradleup.shadow") version ("9.3.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    implementation("org.reflections:reflections:0.10.2")

    // Misc
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")
}

val buildPropsFile = layout.projectDirectory.file("src/main/resources/build.properties").asFile
val buildNumberFile = layout.projectDirectory.file("build-number.properties").asFile

fun readBuildNumber(): Int {
    if (!buildNumberFile.exists()) return 1
    return buildNumberFile.readLines()
        .firstOrNull { it.startsWith("buildNumber=") }
        ?.substringAfter("=")
        ?.trim()
        ?.toIntOrNull()
        ?.plus(1)
        ?: 1
}

// These are all computed once at configuration time and captured as plain values.
val buildAuthor: String = (findProperty("buildAuthor") ?: "KoolFreedom").toString()
val buildNumber: Int    = readBuildNumber()
val buildDate: String   = SimpleDateFormat("M/dd/yyyy 'at' h:mm:ss aa zzz").format(Date())
val buildVersion: String = version.toString()

tasks {
    // Update build.properties with build information
    processResources {
        inputs.property("buildAuthor",  buildAuthor)
        inputs.property("buildNumber",  buildNumber)
        inputs.property("buildVersion", buildVersion)
        inputs.property("buildDate",    buildDate)

        filesMatching("build.properties") {
            expand(mapOf(
                "buildAuthor"  to buildAuthor,
                "buildNumber"  to buildNumber,
                "buildVersion" to buildVersion,
                "buildDate"    to buildDate
            ))
        }

        filesMatching("paper-plugin.yml") {
            expand(mapOf("buildVersion" to buildVersion))
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
        relocate("org.reflections", "eu.koolfreedom.libs.reflections")
        relocate("javassist", "eu.koolfreedom.libs.javassist")
        finalizedBy("incrementBuildNumber")
    }

    build {
        dependsOn(shadowJar)
    }
}

tasks.register("incrementBuildNumber") {
    description = "Writes the incremented build number back to build-number.properties for the next build"

    // Capture everything needed as local vals so the doLast lambda is a pure
    // closure over plain values and never references `project`.
    val outputFile = buildNumberFile
    val nextNumber = buildNumber

    outputs.file(outputFile)

    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText("buildNumber=$nextNumber\n")
    }
}