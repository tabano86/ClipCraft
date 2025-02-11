plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "1.8.21"
}

group = "com.clipcraft"
version = System.getenv("PLUGIN_VERSION") ?: "2.0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

intellij {
    version.set("2022.3")
    type.set("IC") // IntelliJ Community
    plugins.set(listOf("Git4Idea"))
    instrumentCode.set(false)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.intellij.tasks.PatchPluginXmlTask> {
    sinceBuild.set("223")
    untilBuild.set("243.*")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs += listOf("-Xdisable-coroutines-java-agent")
}
