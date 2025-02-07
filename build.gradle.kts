plugins {
    id("java")
    // Latest IntelliJ Gradle plugin
    id("org.jetbrains.intellij") version "1.17.4"
    // Kotlin 1.8.21 or newer
    kotlin("jvm") version "1.8.21"
}

group = "com.clipcraft"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    // Use IntelliJ 2022.3, adjust as needed
    version.set("2022.3")
    type.set("IC")
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

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    // IntelliJ 2022.3 => 223.x
    sinceBuild.set("223")
    untilBuild.set("232.*")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
