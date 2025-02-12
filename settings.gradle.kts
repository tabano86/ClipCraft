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
            // Apply the default conventional commit types.
            defaultTypes()
            // You can add custom types if needed, for example:
            // types("foo", "bar")
        }
    }
    // Automatically create or overwrite hooks on project import/update.
    createHooks()
}
