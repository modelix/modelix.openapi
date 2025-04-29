

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
        val version = gitVersion()
        versionFile.writeText(version)
        version
    }
}

group = "org.modelix"

subprojects {
    group = rootProject.group
    version = rootProject.version
    apply(plugin = "maven-publish")

    repositories {
        mavenLocal()
        maven { url = uri("https://artifacts.itemis.cloud/repository/maven-mps/") }
        mavenCentral()
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
