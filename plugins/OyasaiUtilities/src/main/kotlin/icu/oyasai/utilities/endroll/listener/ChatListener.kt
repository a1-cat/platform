package icu.oyasai.utilities.endroll.listener

import icu.oyasai.utilities.endroll.gui.ChatInputHandler
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class ChatListener(private val chatInputHandler: ChatInputHandler) : Listener {

  @EventHandler(priority = EventPriority.LOWEST)
  fun onPaperChat(event: AsyncChatEvent) {
    val player = event.player
    if (!chatInputHandler.hasSession(player.uniqueId)) return

    event.isCancelled = true
    val messageText = PlainTextComponentSerializer.plainText().serialize(event.message())
    chatInputHandler.handleInput(player, messageText)
  }

  @Suppress("DEPRECATION")
  @EventHandler(priority = EventPriority.LOWEST)
  fun onLegacyChat(event: AsyncPlayerChatEvent) {
    val player = event.player
    if (!chatInputHandler.hasSession(player.uniqueId)) return

    event.isCancelled = true
    val messageText = event.message
    chatInputHandler.handleInput(player, messageText)
  }
}
