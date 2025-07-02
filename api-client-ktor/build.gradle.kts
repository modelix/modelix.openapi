import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    base
    `maven-publish`
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.kotlin.serialization)
    kotlin("jvm")
}

val generatorTask = tasks.register("generateKtorClient", GenerateTask::class) {
    group = "openapi tools"
    configOptions = mapOf(
        "library" to "jvm-ktor",
        "serializationLibrary" to "kotlinx_serialization"
    )
    gitUserId = "modelix"
    gitRepoId = "modelix.openapi"
    generatorName = "kotlin"
    inputSpec = project(":redocly")
        .layout.projectDirectory
        .file("bundled/git-connector-v1.yaml")
        .asFile.absolutePath
    outputDir =
        layout.buildDirectory
            .dir("generated/ktor")
            .get()
            .asFile.absolutePath
    //importMappings.put("UUID", "kotlin.uuid.Uuid")
}

val generatedKotlinSrc = project.layout.buildDirectory.dir("generated/ktor/src/main/kotlin")
sourceSets.main {
    kotlin.srcDir(generatedKotlinSrc)
}

dependencies {
    api(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
}

tasks.compileKotlin {
    dependsOn(generatorTask)
}