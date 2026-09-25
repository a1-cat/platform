package icu.oyasai.utilities.hologram.command

import icu.oyasai.utilities.hologram.gui.MainMenuGui
import icu.oyasai.utilities.hologram.listener.ToolListener
import icu.oyasai.utilities.hologram.manager.HologramManager
import icu.oyasai.utilities.hologram.util.TextFormatter
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

class HologramCommand(
    private val plugin: JavaPlugin,
    private val manager: HologramManager,
    private val toolListener: ToolListener,
) : CommandExecutor, TabCompleter {

  override fun onCommand(
      sender: CommandSender,
      command: Command,
      label: String,
      args: Array<out String>,
  ): Boolean {
    if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
      sendHelp(sender, label)
      return true
    }

    when (args[0].lowercase()) {
      "tool" -> {
        if (sender !is Player) {
          sender.sendMessage("このコマンドはプレイヤーのみ実行できます。")
          return true
        }
        val tool = toolListener.getHologramTool()
        sender.inventory.addItem(tool)
        sender.playSound(sender.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f)
        sender.sendMessage(TextFormatter.format("&a[ホログラム] &dホロクリスタル&aを受け取りました！"))
        sender.sendMessage(
            TextFormatter.format("&7Shift+右クリックで設置/メニュー、左クリックで調整切替、右クリックで数値変更ができます。")
        )
      }

      "menu" -> {
        if (sender !is Player) {
          sender.sendMessage("このコマンドはプレイヤーのみ実行できます。")
          return true
        }

        // 視線先または近傍のホログラムを検索
        val holo = manager.findTargetHologram(sender, 10.0)
        if (holo == null) {
          // 視線先にない場合、自分が所有する直近のホログラムを探す
          val myHolo =
              manager.getPlayerHolograms(sender.uniqueId).minByOrNull {
                if (it.location.world == sender.world) it.location.distanceSquared(sender.location)
                else Double.MAX_VALUE
              }
          if (
              myHolo != null &&
                  myHolo.location.world == sender.world &&
                  myHolo.location.distance(sender.location) <= 15.0
          ) {
            sender.playSound(sender.location, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f)
            MainMenuGui.open(sender, myHolo)
            return true
          }

          sender.sendMessage(TextFormatter.format("&c[ホログラム] &7近くに設定可能なホログラムが見つかりませんでした。"))
          sender.sendMessage(
              TextFormatter.format("&7ホログラムに近づいて視線を合わせるか、ホロクリスタルでShift+右クリックしてください。")
          )
          return true
        }

        if (holo.owner != sender.uniqueId && !sender.hasPermission("endrollhologram.admin")) {
          sender.sendMessage(TextFormatter.format("&c[ホログラム] &7他のプレイヤーが作成したホログラムは編集できません。"))
          return true
        }

        sender.playSound(sender.location, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f)
        MainMenuGui.open(sender, holo)
      }

      "list" -> {
        if (sender !is Player) {
          sender.sendMessage("このコマンドはプレイヤーのみ実行できます。")
          return true
        }
        val myHolos = manager.getPlayerHolograms(sender.uniqueId)
        if (myHolos.isEmpty()) {
          sender.sendMessage(TextFormatter.format("&e[ホログラム] &7設置済みのホログラムはありません。"))
          return true
        }

        sender.sendMessage(TextFormatter.format("&6=== 設置済みホログラム一覧 (${myHolos.size}件) ==="))
        myHolos.forEachIndexed { i, h ->
          val loc = h.location
          val firstLine = h.lines.firstOrNull() ?: "(テキストなし)"
          sender.sendMessage(
              TextFormatter.format(
                  "&e${i + 1}. &f${loc.world?.name ?: "world"} (${loc.blockX}, ${loc.blockY}, ${loc.blockZ}) - &r$firstLine"
              )
          )
        }
      }

      "angle",
      "step" -> {
        if (sender !is Player) {
          sender.sendMessage("このコマンドはプレイヤーのみ実行できます。")
          return true
        }

        if (args.size < 2) {
          val current = toolListener.getYawStep(sender.uniqueId)
          sender.sendMessage(
              TextFormatter.format(
                  "&b[ホログラム] &7現在の回転ステップ角度: &e${current}°\n" +
                      "&7変更方法: &e/$label angle <角度>\n" +
                      "&7デフォルトに戻す: &e/$label angle reset\n" +
                      "&7例: &f/$label angle 1 &7(1度ずつ微調整), &f/$label angle 5 &7(5度ずつ), &f/$label angle 15 &7(標準)"
              )
          )
          return true
        }

        if (args[1].equals("reset", ignoreCase = true)) {
          toolListener.setYawStep(sender.uniqueId, 15.0f)
          sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
          sender.sendMessage(
              TextFormatter.format(
                  "&a[ホログラム] &f回転ステップ角度をデフォルト値(&e15.0°&f)にリセットしました！\n" +
                      "&7ホロクリスタルで向きを調整する際、1クリックで15度ずつ回転します。"
              )
          )
          return true
        }

        val step = args[1].toFloatOrNull()
        if (step == null || step < 0.1f || step > 180.0f) {
          sender.sendMessage(
              TextFormatter.format("&c[ホログラム] &7角度は 0.1 〜 180.0 の範囲の数値、または reset を指定してください。")
          )
          return true
        }

        toolListener.setYawStep(sender.uniqueId, step)
        sender.playSound(sender.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f)
        sender.sendMessage(
            TextFormatter.format(
                "&a[ホログラム] &f回転ステップ角度を &e${step}° &fに設定しました！\n" +
                    "&7ホロクリスタルで向きを調整する際、1クリックでこの角度ずつ回転します。"
            )
        )
      }

      "reload" -> {
        if (!sender.hasPermission("endrollhologram.admin")) {
          sender.sendMessage(TextFormatter.format("&c権限がありません。"))
          return true
        }
        plugin.reloadConfig()
        manager.loadAll()
        sender.sendMessage(TextFormatter.format("&a[ホログラム] &f設定およびホログラムデータを再読み込みしました。"))
      }

      else -> {
        sendHelp(sender, label)
      }
    }

    return true
  }

  private fun sendHelp(sender: CommandSender, label: String) {
    sender.sendMessage(TextFormatter.format("&b&l=== EndRoll Hologram プラグイン ==="))
    sender.sendMessage(TextFormatter.format("&e/$label tool &7- 専用ツール「ホロクリスタル」を取得"))
    sender.sendMessage(
        TextFormatter.format("&e/$label angle <角度|reset> &7- 1クリックあたりの回転角度を設定 (例: 1, 5, 15, reset)")
    )
    sender.sendMessage(TextFormatter.format("&e/$label menu &7- 近くのホログラムの設定メニューを開く"))
    sender.sendMessage(TextFormatter.format("&e/$label list &7- 自分が設置したホログラム一覧を表示"))
    if (sender.hasPermission("endrollhologram.admin")) {
      sender.sendMessage(TextFormatter.format("&c/$label reload &7- 設定とデータをリロード (管理者)"))
    }
  }

  override fun onTabComplete(
      sender: CommandSender,
      command: Command,
      alias: String,
      args: Array<out String>,
  ): MutableList<String> {
    if (args.size == 1) {
      val candidates = mutableListOf("tool", "angle", "menu", "list", "help")
      if (sender.hasPermission("endrollhologram.admin")) {
        candidates.add("reload")
      }
      return candidates.filter { it.startsWith(args[0].lowercase()) }.toMutableList()
    }
    if (
        args.size == 2 &&
            (args[0].equals("angle", ignoreCase = true) ||
                args[0].equals("step", ignoreCase = true))
    ) {
      return listOf("reset", "1", "5", "10", "15", "45", "90")
          .filter { it.startsWith(args[1].lowercase()) }
          .toMutableList()
    }
    return mutableListOf()
  }
}
