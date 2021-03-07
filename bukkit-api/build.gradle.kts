plugins{
    id("maven-publish")
}

java{
    withSourcesJar()
    withJavadocJar()
}

repositories {
    maven{url = uri("https://papermc.io/repo/repository/maven-public/")}
}

dependencies {
    api(project(":api"))
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    testApi("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    testApi(project(":api"))
}

publishing {
    publications {
        create<MavenPublication>("maven"){
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri(System.getenv("PLU_PUBLIC_URL"))

            credentials {
                username = System.getenv("PLU_PUBLIC_PUSH_USER")
                password = System.getenv("PLU_PUBLIC_PUSH_PASSWORD")
            }
        }
    }
}