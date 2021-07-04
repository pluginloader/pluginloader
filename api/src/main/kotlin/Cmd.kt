package pluginloader.api

typealias Args = Array<String>

//@Cmd("lol") internal fun command(sender: CmdSender, args: Args){}
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cmd(val command: String, val aliases: Array<String> = [])

interface CmdSender{
    fun sendMessage(string: String)
}