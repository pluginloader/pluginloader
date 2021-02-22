package pluginloader.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.net.URI

class PaperPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.repositories.add(project.repositories.maven{it.url = URI("https://papermc.io/repo/repository/maven-public/")})
        project.dependencies.add("compileOnly", "pluginloader:bukkit-api:${project.properties["pluginloaderVersion"]}")
        project.dependencies.add("compileOnly", "com.destroystokyo.paper:paper-api:${project.properties["paperVersion"]}-R0.1-SNAPSHOT")
    }
}