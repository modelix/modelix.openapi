import ch.acanda.gradle.fabrikt.FabriktGenerateTask

plugins {
    kotlin("jvm")
    `maven-publish`
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.fabrikt)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.data.conversion)
}

fabrikt {
    defaults {
        validationLibrary = NoValidation
        model {
            generate = enabled
            serializationLibrary = Kotlin
            includeCompanionObject = enabled
            // ignoreUnknownProperties = enabled
            extensibleEnums = enabled
        }
        controller {
            generate = enabled
            target = Ktor
            authentication = enabled
            suspendModifier = enabled
            completionStage = enabled
        }
        typeOverrides {
            uuid = String
        }
    }
    generate("mavenConnector") {
        apiFile = rootProject.layout.projectDirectory.file("redocly/bundled/maven-connector-v1.yaml")
        basePackage = "org.modelix.services.mavenconnector.stubs"
    }
    generate("gitConnector") {
        apiFile = rootProject.layout.projectDirectory.file("redocly/bundled/git-connector-v1.yaml")
        basePackage = "org.modelix.services.gitconnector.stubs"
    }
    generate("repository") {
        apiFile = rootProject.layout.projectDirectory.file("redocly/bundled/repository-v3.yaml")
        basePackage = "org.modelix.services.repository.stubs"
    }
    generate("workspaces") {
        apiFile = rootProject.layout.projectDirectory.file("redocly/bundled/workspaces-v1.yaml")
        basePackage = "org.modelix.services.workspaces.stubs"
    }
}

tasks.withType<FabriktGenerateTask> {
    dependsOn(":redocly:npm_run_bundle")
}

val generatedKotlinSrc = project.layout.buildDirectory.dir("generated/sources/fabrikt/src/main/kotlin")
sourceSets.main {
    kotlin.srcDir(generatedKotlinSrc)
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("apiServerStubs") {
            from(components["java"])
        }
    }
}

tasks.processResources {
    dependsOn(tasks.fabriktGenerate)
}
tasks.compileKotlin {
    dependsOn(tasks.fabriktGenerate)
}

tasks.named("sourcesJar") {
    dependsOn(tasks.fabriktGenerate)
}
