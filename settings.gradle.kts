pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "mgn"
include(":app")
include(":core:model")
include(":core:engine")
include(":core:data")
include(":core:audio")
include(":design")
include(":content")
include(":tools:balance-sim")
include(":feature:setup")
include(":feature:command")
include(":feature:economy")
include(":feature:diplomacy")
include(":feature:development")
include(":benchmark")
