package pluginloader.api

import org.bukkit.configuration.ConfigurationSection
import java.sql.Connection
import java.sql.DriverManager

class Connector(host: String, port: Int, database: String, private val user: String, private val password: String) {
    private val url = "jdbc:mysql://$host:$port/$database"
    private var connection: Connection? = null

    constructor(): this("localhost", 3306, "database", "user", "password")

    fun connection(): Connection {
        if(connection == null || connection!!.isClosed) connection = connect()
        return connection!!
    }

    fun close(){
        if(connection == null)return
        if(connection!!.isClosed)return
        connection!!.close()
    }

    private fun connect() = DriverManager.getConnection(url, user, password)

    companion object{
        fun parse(section: ConfigurationSection) =
                Connector(section.getString("host"),
                section.getInt("port"), section.getString("database"),
                section.getString("user"), section.getString("password"))
    }
}