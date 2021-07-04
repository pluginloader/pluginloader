plugins{
    id("java-gradle-plugin")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

repositories {
    jcenter()
    maven{url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")}
    maven{url = uri("https://repo.codemc.org/repository/maven-public/")}
}

dependencies {
    api(project(":shared"))
    api(project(":bukkit-api"))
    api("de.tr7zw:item-nbt-api:2.8.0")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")

    //testApi(files("../../libs/patched_1.12.2.jar", "../../libs/spigot_1.16.3.jar"))
    testApi("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        dependsOn("jar")
        archiveFileName.set("${rootProject.name}-${rootProject.version}-bukkit.jar")
        relocate("de.tr7zw.changeme.nbtapi", "pluginloader.internal.nbtapi")
    }
    named("assemble"){dependsOn("shadowJar")}
}

tasks{
    named<Jar>("jar"){
        archiveFileName.set("${rootProject.name}-${rootProject.version}-bukkit-raw.jar")
    }
}