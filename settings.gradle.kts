rootProject.name = "clipcraft-plugin"

plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.20"
}

if (System.getenv("CI") == null) {
    gitHooks {
        preCommit {
            from {
                """
                #!/usr/bin/env bash
                cd "$(git rev-parse --show-toplevel)" || exit 1
                """.trimIndent()
            }
            tasks("spotlessCheck", "detekt")
            appendScript {
                """
                if [ $? -ne 0 ]; then
                  echo "❌ Pre-commit checks failed! Please fix issues before committing."
                fi
                """.trimIndent()
            }
        }
        commitMsg {
            conventionalCommits { defaultTypes() }
        }
        createHooks(overwriteExisting = true)
    }
} else {
    println("CI environment detected; skipping Git hook installation.")
}
