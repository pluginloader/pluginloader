plugins {
    kotlin("jvm") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
}

repositories{
    mavenLocal()
    mavenCentral()
}

group = "pluginloader"
version = "1.8.22"

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
        compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.1.0")
        compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.1.0")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
        testApi("junit:junit:4.12")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-core:1.1.0")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.1.0")
        testApi("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.1.0")
        testApi("com.charleskorn.kaml:kaml:0.26.0"){
            exclude("org.jetbrains.kotlin")
            exclude("org.jetbrains.kotlinx")
            exclude("org.jetbrains.annotations")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes", "-Xuse-14-inline-classes-mangling-scheme")
    }
}