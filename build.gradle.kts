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
    // Target IntelliJ IDEA 2022.3 (Community Edition).
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
    sinceBuild.set("182")
    untilBuild.set("*")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
