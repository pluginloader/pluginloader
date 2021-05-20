plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.2.1")
    api(gradleApi())
}

gradlePlugin {
    plugins {
        create("api") {
            id = "pluginloader.api"
            implementationClass = "pluginloader.gradle.ApiPlugin"
        }
        create("paper") {
            id = "pluginloader.paper"
            implementationClass = "pluginloader.gradle.PaperPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven"){
            from(components["java"])
        }
    }

    repositories {
        if(System.getenv("PLU_PUBLIC_URL") != null) {
            maven {
                url = uri(System.getenv("PLU_PUBLIC_URL"))

                credentials {
                    username = System.getenv("PLU_PUBLIC_PUSH_USER")
                    password = System.getenv("PLU_PUBLIC_PUSH_PASSWORD")
                }
            }
        }
    }
}