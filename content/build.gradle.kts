plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.serialization.json)
    testImplementation(project(":core:model"))
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

tasks.withType<Test> {
    useJUnit()
}
