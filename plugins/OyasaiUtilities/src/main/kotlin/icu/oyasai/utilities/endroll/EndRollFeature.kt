package icu.oyasai.utilities.endroll

import icu.oyasai.utilities.endroll.command.HologramCommand
import icu.oyasai.utilities.endroll.engine.ScrollAnimationEngine
import icu.oyasai.utilities.endroll.gui.ChatInputHandler
import icu.oyasai.utilities.endroll.listener.ChatListener
import icu.oyasai.utilities.endroll.listener.GuiListener
import icu.oyasai.utilities.endroll.listener.ToolListener
import icu.oyasai.utilities.endroll.manager.HologramManager
import org.bukkit.plugin.java.JavaPlugin

object EndRollFeature {

  lateinit var manager: HologramManager
  lateinit var animationEngine: ScrollAnimationEngine
  lateinit var chatInputHandler: ChatInputHandler
  lateinit var toolListener: ToolListener
  lateinit var guiListener: GuiListener
  lateinit var chatListener: ChatListener

  fun onEnable(plugin: JavaPlugin) {
    manager = HologramManager(plugin)
    manager.loadAll()

    animationEngine = ScrollAnimationEngine(plugin, manager)
    animationEngine.start()

    chatInputHandler = ChatInputHandler(plugin, manager)
    toolListener = ToolListener(plugin, manager)
    guiListener = GuiListener(plugin, manager, animationEngine, chatInputHandler)
    chatListener = ChatListener(chatInputHandler)

    val pm = plugin.server.pluginManager
    pm.registerEvents(toolListener, plugin)
    pm.registerEvents(guiListener, plugin)
    pm.registerEvents(chatListener, plugin)

    val command = HologramCommand(plugin, manager, toolListener)
    listOf("erh", "endrollhologram", "hologram").forEach { cmdName ->
      plugin.getCommand(cmdName)?.let {
        it.setExecutor(command)
        it.tabCompleter = command
      }
    }

    plugin.logger.info("エンドロールホログラム機能を有効化しました。")
  }

  fun onDisable() {
    if (::animationEngine.isInitialized) {
      animationEngine.stop()
    }
    if (::manager.isInitialized) {
      manager.cleanup()
    }
  }
}
