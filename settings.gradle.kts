pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2/")
    }
}
rootProject.name = "ClipCraft"

// Apply the pre-commit git hooks plugin without immediately applying it.
plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.20" apply false
}

if (gradle.startParameter.projectProperties.containsKey("gitHooks")) {
    gitHooks {
        commitMsg {
            conventionalCommits {
                defaultTypes()
            }
        }
        createHooks()
    }
}
