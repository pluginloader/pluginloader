plugins {
    id("maven-publish")
}

java{
    withSourcesJar()
    withJavadocJar()
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