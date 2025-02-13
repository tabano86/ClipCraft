plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.20"
}

gitHooks {
    preCommit {
        // Ensure the hook runs from the project root (so that gradlew is found)
        from {
            """
            #!/usr/bin/env bash
            cd "$(git rev-parse --show-toplevel)" || exit 1
            """.trimIndent()
        }
        // Run the required tasks
        tasks("spotlessCheck", "detekt")
        appendScript {
            """
            if [ ${'$'}? -ne 0 ]; then
              echo "❌ Pre-commit checks failed! Please run './gradlew spotlessCheck detekt' for more details and fix the issues before committing."
            fi
            """.trimIndent()
        }
    }
    commitMsg {
        conventionalCommits { }
    }
    createHooks(overwriteExisting = true)
}
