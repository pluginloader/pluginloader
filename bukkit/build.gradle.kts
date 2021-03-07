dependencies {
    api(project(":shared"))
    api(project(":bukkit-api"))
    compileOnly(files("../../libs/patched_1.12.2.jar", "../../libs/spigot_1.16.3.jar"))

    testApi(files("../../libs/patched_1.12.2.jar", "../../libs/spigot_1.16.3.jar"))
    testApi("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
}

tasks.withType<Jar>{
    doFirst{
        project.file("build/classes/kotlin/main/plugin.yml").writeText("""main: "pluginloader.internal.bukkit.JavaPlugin"
version: "${rootProject.version}"
name: "PluginLoader"
""")
    }
    archiveFileName.set("${rootProject.name}-${rootProject.version}-bukkit.jar")
    from(configurations.runtimeClasspath.get().map{if(it.isDirectory) it else zipTree(it)})
}