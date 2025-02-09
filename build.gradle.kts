plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "1.8.21"
}

group = "com.clipcraft"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2022.3")
    type.set("IC")
    plugins.set(listOf("Git4Idea"))
    // Disable code instrumentation to produce a valid plugin descriptor.
    instrumentCode.set(false)
}


dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Update the patchPluginXml task to match the new build range.
tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    sinceBuild.set("223")
    untilBuild.set("243.*")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    // Disable the coroutines Java agent to avoid the missing agent error.
    kotlinOptions.freeCompilerArgs += listOf("-Xdisable-coroutines-java-agent")
}
