pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        jcenter()
    }
}

rootProject.name = "pluginloader"
include("api", "bukkit-api", "shared", "bukkit", "bukkit-wrapper", "standalone")