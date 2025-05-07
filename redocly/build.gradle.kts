import com.github.gradle.node.npm.task.NpmTask
import org.gradle.kotlin.dsl.register

plugins {
    base
    alias(libs.plugins.node)
}

tasks.assemble {
    dependsOn("npm_run_bundle")
}

val bundledDir = layout.projectDirectory.dir("bundled")
val publicationFile = bundledDir.file("modelix-openapi-specifications-${project.version}.tgz")

val updateVersionTask = tasks.register<NpmTask>("updatePackageVersion") {
    workingDir.set(bundledDir.asFile)
    npmCommand = listOf("version")
    args.set(listOf(
        project.version.toString(),
        "--allow-same-version"
    ))
}

val npmPackTask =
    tasks.register<NpmTask>("npmPack") {
        group = "build"
        dependsOn("npm_run_bundle")
        dependsOn(updateVersionTask)

        workingDir.set(bundledDir.asFile)
        npmCommand = listOf("pack")
    }

val npmPublishTask =
    tasks.register<NpmTask>("npmPublish") {
        group = "publishing"
        dependsOn(npmPackTask)

        inputs.file(publicationFile)

        workingDir.set(bundledDir.asFile)
        npmCommand.set(listOf("publish"))
        args.set(
            listOf(
                publicationFile.asFile.absolutePath,
                "--registry=https://artifacts.itemis.cloud/repository/npm-open/",
                "--//artifacts.itemis.cloud/repository/npm-open/:_authToken=${project.findProperty(
                    "artifacts.itemis.cloud.npm.token",
                )}",
            ),
        )
    }

tasks.assemble {
    dependsOn(npmPackTask)
}

tasks.publish {
    dependsOn(npmPublishTask)
}