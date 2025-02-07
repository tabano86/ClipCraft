plugins {
    id("java")
    // Use org.jetbrains.intellij 1.17.4
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "1.8.21"
}

group = "com.clipcraft"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    // IntelliJ 2022.3
    version.set("2022.3")
    type.set("IC") // Community Edition
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
    sinceBuild.set("223")
    untilBuild.set("223.*")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
