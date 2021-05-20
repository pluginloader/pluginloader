plugins {
    kotlin("jvm") version "1.5.0"
    kotlin("plugin.serialization") version "1.5.0"
}

repositories{
    mavenLocal()
    mavenCentral()
}

group = "pluginloader"
version = "1.8.24"

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
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
        testApi("junit:junit:4.12")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.2.1")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.2.1")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes", "-Xuse-14-inline-classes-mangling-scheme")
    }
}