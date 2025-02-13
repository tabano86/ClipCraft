plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.20"
}


gitHooks {
    preCommit {
        tasks("spotlessApply")
    }
    commitMsg {
        conventionalCommits { }
    }
    createHooks()
}
