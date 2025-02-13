plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.20"
}

gitHooks {
    commitMsg {
        conventionalCommits { }
    }
    preCommit {
        from {
            "spotlessApply"
        }
    }

    createHooks()
}

gitHooks.preCommit {
    tasks("ktlintCheck")
}