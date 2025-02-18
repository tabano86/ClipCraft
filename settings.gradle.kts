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
                    echo "❌ Pre-commit checks failed! Please run './gradlew spotlessCheck detekt' for more details and fix the issues before committing."
                fi
                """.trimIndent()
            }
        }
        commitMsg {
            conventionalCommits {
                // Enable full conventional commit types: fix, feat, build, chore, ci, docs, perf, refactor, revert, style, test.
                defaultTypes()
            }
        }
        createHooks(overwriteExisting = true)
    }
} else {
    println("CI environment detected; skipping Git hook installation.")
}
