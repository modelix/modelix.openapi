import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.pnpm.task.PnpmTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    base
    `maven-publish`
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.node)
}

val publishAllClients by tasks.registering {
    group = "publishing"
}
val packageAllClients by tasks.registering {
    group = "build"
}

// Helper function to register tasks for a specific client type
fun registerNpmPackageTasks(
    clientType: String, // e.g., "fetch", "axios", "redux"
    npmPackageName: String, // e.g., "modelix-api-client-ts-fetch"
    openApiGeneratorName: String, // e.g., "typescript-fetch"
) {
    val clientBuildDir = layout.buildDirectory.dir("generate").map { it.dir(openApiGeneratorName) }
    val publicationFile = clientBuildDir.map { it.file("modelix-${npmPackageName}-${project.version}.tgz") }

    val generatorTask = tasks.register("generateTypescript$clientType", GenerateTask::class) {
        dependsOn(":redocly:npm_run_join")
        group = "openapi tools"
        inputSpec = project(":redocly").layout.buildDirectory.file("joined.yaml").get().asFile.absolutePath
        configOptions = mapOf(
            "npmRepository" to "https://artifacts.itemis.cloud/repository/npm-open/",
            "npmVersion" to project.version.toString(),
            "npmName" to "@modelix/$npmPackageName",
        )
        gitUserId = "modelix"
        gitRepoId = "modelix.openapi"
        generatorName = openApiGeneratorName
        outputDir = layout.buildDirectory.dir("generate/$openApiGeneratorName").get().asFile.absolutePath
    }

    // Task to run 'pnpm install'
    val pnpmInstallTask = tasks.register<PnpmTask>("pnpmInstall${clientType.capitalize()}") {
        group = "build"
        description = "Run pnpm install for $openApiGeneratorName client"
        dependsOn(generatorTask) // Must run after generation

        workingDir.set(clientBuildDir)
        pnpmCommand = listOf("install")

        // Define inputs/outputs for better up-to-date checks and caching
        //inputs.dir(clientBuildDir.map { it.dir("src") }).withPathSensitivity(PathSensitivity.RELATIVE) // Source might change
        inputs.file(clientBuildDir.map { it.file("package.json") }).withPathSensitivity(PathSensitivity.RELATIVE)
        // Add pnpm-lock.yaml if you commit/use it
        // inputs.file(clientBuildDir.map { it.file("pnpm-lock.yaml") }).optional().withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir(clientBuildDir.map { it.dir("node_modules") })
        // If 'pnpm install' triggers a build (via 'prepare' script) that creates a 'dist' folder:
        // outputs.dir(clientBuildDir.map { it.dir("dist") })
    }

    // Task to run 'npm pack'
    val npmPackTask = tasks.register<NpmTask>("npmPack${clientType.capitalize()}") {
        group = "build"
        description = "Run npm pack for $npmPackageName"
        dependsOn(pnpmInstallTask) // Must run after pnpm install

        // npm pack creates the .tgz in the *current* working directory
        workingDir.set(clientBuildDir.get().asFile) // Run npm pack from the project root
        npmCommand = listOf("pack")
        // Argument is the path to the directory containing package.json
        args.set(listOf(clientBuildDir.get().asFile.absolutePath))

        inputs.dir(clientBuildDir)
        inputs.files(pnpmInstallTask.map { it.outputs.files })

        outputs.file(publicationFile)
    }

    val publishPackageTasks = tasks.register<NpmTask>("publish${clientType.capitalize()}") {
        group = "publishing" // Standard Gradle group for publishing
        description = "Publishes the $npmPackageName package to the configured NPM registry."
        dependsOn(npmPackTask) // Must run after the package is copied

        // Input: The specific .tgz file to be published
        inputs.file(publicationFile)

        workingDir.set(project.projectDir) // Can run from project root
        npmCommand.set(listOf("publish"))
        // Argument is the path to the .tgz file in the build/packages directory
        args.set(listOf(
            publicationFile.get().asFile.absolutePath,
            "--registry=https://artifacts.itemis.cloud/repository/npm-open/",
            "--//artifacts.itemis.cloud/repository/npm-open/:_authToken=${project.findProperty("artifacts.itemis.cloud.npm.token").toString()}"
        ))

        // Add --access public if publishing public packages to npmjs.com scoped or not
        // if (isPublicPackage) {
        //    args.add("--access")
        //    args.add("public")
        // }

        // Ensure registry/auth is configured via .npmrc or node {} block
        doFirst {
            logger.lifecycle("Attempting to publish ${publicationFile.get().asFile.name}...")
            logger.info("Ensure NPM registry and authentication are configured (e.g., via .npmrc)")
            if (!publicationFile.get().asFile.exists()) {
                throw GradleException("Package file to publish does not exist: ${publicationFile.get().asFile.absolutePath}")
            }
        }
    }

    packageAllClients {
        dependsOn(npmPackTask)
    }
    publishAllClients {
        dependsOn(publishPackageTasks)
    }
}

// Register the tasks for each client type
registerNpmPackageTasks(
    clientType = "fetch",
    npmPackageName = "api-client-ts-fetch",
    openApiGeneratorName = "typescript-fetch"
)
registerNpmPackageTasks(
    clientType = "axios",
    npmPackageName = "api-client-ts-axios",
    openApiGeneratorName = "typescript-axios"
)
registerNpmPackageTasks(
    clientType = "redux",
    npmPackageName = "api-client-ts-redux",
    openApiGeneratorName = "typescript-redux-query"
)

tasks.assemble {
    dependsOn(packageAllClients)
}

tasks.publish {
    dependsOn(publishAllClients)
}