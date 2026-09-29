plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:engine"))
    implementation(project(":content"))
}

application {
    mainClass.set("studio.mgn.sim.BalanceSimKt")
}

tasks.register<JavaExec>("balanceBot") {
    group = "verification"
    description = "Automated playthrough: 5 seeds x 120 turns x 3 behaviors."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("studio.mgn.sim.BotPlaythroughKt")
}
