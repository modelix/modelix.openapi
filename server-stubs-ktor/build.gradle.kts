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
    generate("modelix") {
        apiFile = project(":redocly").layout.buildDirectory.file("joined.yaml")
        basePackage = "org.modelix.service.workspaces"
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
    }
}

tasks.withType<FabriktGenerateTask> {
    dependsOn(":redocly:npm_run_join")
}

val generatedKotlinSrc = project.layout.buildDirectory.dir("generated/sources/fabrikt/src/main/kotlin")
sourceSets.main {
    kotlin.srcDir(generatedKotlinSrc)
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

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(11)
}

publishing {
    publications {
        create<MavenPublication>("apiServerStubs") {
            groupId = project.group as String
            artifactId = "api-server-stubs-ktor"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
