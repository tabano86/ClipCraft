pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2/")
    }
}
rootProject.name = "ClipCraft"

plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.20"
}

gitHooks {
    commitMsg {
        conventionalCommits {
            // Use default Conventional Commit types (adjust if needed)
            defaultTypes()
        }
    }
    // Automatically create the hooks on project import/update.
    createHooks()
}
