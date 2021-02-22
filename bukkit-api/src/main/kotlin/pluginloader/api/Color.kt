package pluginloader.api

import net.md_5.bungee.api.ChatColor

enum class Color(
    val teamName: String,
    val woolData: Int,
    val dyeData: Int,
    val color: Int,
    val fireworkColor: Int,
    val chatFormat: String,
    val chatFormatCode: Int,
    val chatColor: ChatColor
) {
    WHITE("Белые", 0, 15, 16383998, 15790320, "§f", 15, ChatColor.WHITE),
    ORANGE("Оранжевые", 1, 14, 16351261, 15435844, "§6", 6, ChatColor.GOLD),
    MAGENTA("Пурпурные", 2, 13, 13061821, 12801229, "§d", 13, ChatColor.LIGHT_PURPLE),
    AQUA("Аквамариновые", 3, 12, 3847130, 6719955, "§b", 11, ChatColor.AQUA),
    YELLOW("Жёлтые", 4, 11, 16701501, 14602026, "§e", 14, ChatColor.YELLOW),
    LIME("Лаймовые", 5, 10, 8439583, 4312372, "§a", 10, ChatColor.GREEN),
    PINK("Розовые", 6, 9, 15961002, 14188952, "§c", 12, ChatColor.RED),
    GRAY("Серые", 7, 8, 4673362, 4408131, "§8", 8, ChatColor.DARK_GRAY),
    SILVER("Серебрянные", 8, 7, 10329495, 11250603, "§7", 7, ChatColor.GRAY),
    CYAN("Бирюзовые", 9, 6, 1481884, 2651799, "§3", 3, ChatColor.DARK_AQUA),
    PURPLE("Пурпурные", 10, 5, 8991416, 8073150, "§5", 5, ChatColor.DARK_PURPLE),
    BLUE("Синие", 11, 4, 3949738, 2437522, "§1", 1, ChatColor.DARK_BLUE),
    BROWN("Коричневые", 12, 3, 8606770, 5320730, "§9", 9, ChatColor.BLUE),
    GREEN("Зелёные", 13, 2, 6192150, 3887386, "§2", 2, ChatColor.DARK_GREEN),
    RED("Красные", 14, 1, 11546150, 11743532, "§4", 4, ChatColor.DARK_RED),
    BLACK("Чёрные", 15, 0, 1908001, 1973019, "§0", 0, ChatColor.BLACK);

    override fun toString() = chatFormat
}