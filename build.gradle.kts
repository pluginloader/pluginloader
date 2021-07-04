plugins {
    kotlin("jvm") version "1.5.20"
    kotlin("plugin.serialization") version "1.5.20"
}

repositories{
    mavenLocal()
    mavenCentral()
}

group = "pluginloader"
version = "1.9.0"

subprojects{
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    group = rootProject.group
    version = rootProject.version

    repositories{
        mavenLocal()
        mavenCentral()
    }

    dependencies{
        compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
        compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xuse-14-inline-classes-mangling-scheme")
    }
}