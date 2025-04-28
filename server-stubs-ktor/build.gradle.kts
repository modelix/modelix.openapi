
plugins {
    kotlin("jvm")
    `maven-publish`
    alias(libs.plugins.kotlin.serialization)
    id("ch.acanda.gradle.fabrikt") version "1.15.2"
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.data.conversion)
}

tasks.processResources {
    dependsOn(tasks.fabriktGenerate)
}
tasks.compileKotlin {
    dependsOn(tasks.fabriktGenerate)
}

fabrikt {
    generate("modelix") {
        apiFile = file("specifications/workspace-manager.yaml")
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

val generatedKotlinSrc = project.layout.buildDirectory.dir("generated/sources/fabrikt/src/main/kotlin")
sourceSets["main"].resources.srcDir(generatedKotlinSrc)

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
            version = project.version as String
            from(components["java"])
        }
    }
}
