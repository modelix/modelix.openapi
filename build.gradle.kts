

plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.gitVersion)
    `maven-publish`
}

version = computeVersion().also { println("Version: $it") }

fun computeVersion(): String {
    val versionFile = file("version.txt")
    return if (versionFile.exists()) {
        versionFile.readText().trim()
    } else {
        val gitVersion: groovy.lang.Closure<String> by extra
        gitVersion()
        .let {
            // Normalize the version so that is always a valid NPM version.
            if (it.matches("""\d+\.\d+.\d+-.*""".toRegex())) it else "0.0.1-$it"
        }
        .also { versionFile.writeText(it) }
    }
}

group = "org.modelix"

subprojects {
    group = rootProject.group
    version = rootProject.version
    apply(plugin = "maven-publish")

    repositories {
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
        mavenCentral()
        mavenLocal()
    }

    publishing {
        repositories {
            maven {
                name = "itemis"
                url =
                    if (version.toString().contains("SNAPSHOT")) {
                        uri("https://artifacts.itemis.cloud/repository/maven-mps-snapshots/")
                    } else {
                        uri("https://artifacts.itemis.cloud/repository/maven-mps-releases/")
                    }
                credentials {
                    username = project.findProperty("artifacts.itemis.cloud.user").toString()
                    password = project.findProperty("artifacts.itemis.cloud.pw").toString()
                }
            }
        }
    }
}
