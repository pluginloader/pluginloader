plugins{
    id("java")
}

repositories {
    maven{url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")}
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
}

tasks{
    named<Jar>("jar"){
        archiveFileName.set("${rootProject.name}-wrapper.jar")
    }
}