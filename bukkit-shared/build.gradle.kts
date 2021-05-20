repositories {
    maven{url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")}
    maven{url = uri("https://repo.codemc.org/repository/maven-public/")}
}

dependencies {
    api(project(":shared"))
    api(project(":bukkit-api"))
    api("de.tr7zw:item-nbt-api:2.7.1")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")

    //testApi(files("../../libs/patched_1.12.2.jar", "../../libs/spigot_1.16.3.jar"))
    testApi("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
}