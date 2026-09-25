package icu.oyasai.utilities.endroll.gui

import icu.oyasai.utilities.endroll.manager.HologramManager
import icu.oyasai.utilities.endroll.util.TextFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class ChatInputHandler(
    private val plugin: JavaPlugin,
    private val manager: HologramManager,
) {
  data class InputSession(
      val hologramId: UUID,
      val lineIndex: Int, // -1 indicates adding a new line
  )

  private val sessions = ConcurrentHashMap<UUID, InputSession>()

  fun expectInput(player: Player, hologramId: UUID, lineIndex: Int) {
    sessions[player.uniqueId] = InputSession(hologramId, lineIndex)
    player.closeInventory()
    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f)

    player.sendMessage(TextFormatter.format("&b[ホログラム] &eチャットにテキストを入力してください。"))
    player.sendMessage(
        TextFormatter.format("&7※ カラーコード(&e&&a&7, &e&&l&7, &e&&#RRGGBB&7)使用可。キャンセルは '&ccancel&7'")
    )
  }

  fun cancelInput(player: Player) {
    val session = sessions.remove(player.uniqueId) ?: return
    val data = manager.get(session.hologramId)
    player.sendMessage(TextFormatter.format("&e[ホログラム] &7テキスト入力をキャンセルしました。"))
    if (data != null) {
      Bukkit.getScheduler().runTask(plugin, Runnable { TextEditorGui.open(player, data) })
    }
  }

  fun hasSession(playerId: UUID): Boolean = sessions.containsKey(playerId)

  fun handleInput(player: Player, rawMessage: String): Boolean {
    val session = sessions.remove(player.uniqueId) ?: return false

    if (rawMessage.equals("cancel", ignoreCase = true)) {
      player.sendMessage(TextFormatter.format("&e[ホログラム] &7入力をキャンセルしました。"))
      val data = manager.get(session.hologramId)
      if (data != null) {
        Bukkit.getScheduler().runTask(plugin, Runnable { TextEditorGui.open(player, data) })
      }
      return true
    }

    val data = manager.get(session.hologramId)
    if (data == null) {
      player.sendMessage(TextFormatter.format("&c[ホログラム] &7対象のホログラムが見つかりませんでした。"))
      return true
    }

    if (session.lineIndex == -1) {
      // 新規行追加
      data.lines.add(rawMessage)
      player.sendMessage(TextFormatter.format("&a[ホログラム] &f新しい行を追加しました: &r$rawMessage"))
    } else if (session.lineIndex in data.lines.indices) {
      // 既存行編集
      data.lines[session.lineIndex] = rawMessage
      player.sendMessage(
          TextFormatter.format("&a[ホログラム] &f${session.lineIndex + 1}行目を更新しました: &r$rawMessage")
      )
    }

    // 表示更新 & 保存
    Bukkit.getScheduler()
        .runTask(
            plugin,
            Runnable {
              manager.spawnOrUpdateEntities(data)
              manager.saveAll()
              player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f)
              TextEditorGui.open(player, data)
            },
        )

    return true
  }
}
