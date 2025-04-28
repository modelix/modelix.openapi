plugins {
    base
    alias(libs.plugins.node)
}

tasks.named("npm_run_join") {
    dependsOn("npm_run_bundle")
}
