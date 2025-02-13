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
            """
        }
        // Run the required tasks
        tasks("spotlessCheck", "detekt")
        // Append a custom message if checks fail
        appendScript {
            """
            if [ $? -ne 0 ]; then
              echo "❌ Pre-commit checks failed! Please run './gradlew spotlessCheck detekt' for more details and fix the issues before committing."
            fi
            """
        }
    }
    commitMsg {
        // Initialize with a header to ensure the script is created
        from {
            """
            # Enforce Conventional Commits format.
            """
        }
        // Append a reminder message to help developers format commit messages correctly
        appendScript {
            """
            echo "💡 Reminder: Your commit message should follow Conventional Commits (see https://www.conventionalcommits.org/)."
            """
        }
    }
    // Overwrite any existing hook files automatically
    createHooks(overwriteExisting = true)
}
