plugins{
    id("maven-publish")
}

java{
    withSourcesJar()
    withJavadocJar()
}

repositories {
    maven{url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")}
    maven{url = uri("https://repo.codemc.org/repository/maven-public/")}
}

dependencies {
    api(project(":api"))
    compileOnly("de.tr7zw:item-nbt-api:2.8.0")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
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