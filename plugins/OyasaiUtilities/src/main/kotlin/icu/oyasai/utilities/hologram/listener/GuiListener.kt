package icu.oyasai.utilities.hologram.listener

import icu.oyasai.utilities.hologram.engine.ScrollAnimationEngine
import icu.oyasai.utilities.hologram.gui.ChatInputHandler
import icu.oyasai.utilities.hologram.gui.ColorPaletteGui
import icu.oyasai.utilities.hologram.gui.ColorPaletteHolder
import icu.oyasai.utilities.hologram.gui.DeleteConfirmGui
import icu.oyasai.utilities.hologram.gui.DeleteConfirmHolder
import icu.oyasai.utilities.hologram.gui.MainMenuGui
import icu.oyasai.utilities.hologram.gui.MainMenuHolder
import icu.oyasai.utilities.hologram.gui.TextEditorGui
import icu.oyasai.utilities.hologram.gui.TextEditorHolder
import icu.oyasai.utilities.hologram.manager.HologramManager
import icu.oyasai.utilities.hologram.model.BackgroundType
import icu.oyasai.utilities.hologram.model.DisplayMode
import icu.oyasai.utilities.hologram.model.PlayMode
import icu.oyasai.utilities.hologram.model.TextOrientation
import icu.oyasai.utilities.hologram.util.TextFormatter
import org.bukkit.Sound
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.java.JavaPlugin

class GuiListener(
    private val plugin: JavaPlugin,
    private val manager: HologramManager,
    private val animationEngine: ScrollAnimationEngine,
    private val chatInputHandler: ChatInputHandler,
) : Listener {

  @EventHandler
  fun onInventoryClick(event: InventoryClickEvent) {
    val player = event.whoClicked as? Player ?: return
    val holder = event.inventory.holder ?: return

    when (holder) {
      is MainMenuHolder -> handleMainMenuClick(event, player, holder)
      is TextEditorHolder -> handleTextEditorClick(event, player, holder)
      is DeleteConfirmHolder -> handleDeleteConfirmClick(event, player, holder)
      is ColorPaletteHolder -> handleColorPaletteClick(event, player, holder)
    }
  }

  private fun handleMainMenuClick(
      event: InventoryClickEvent,
      player: Player,
      holder: MainMenuHolder,
  ) {
    event.isCancelled = true
    val data = manager.get(holder.hologramId) ?: return
    val slot = event.rawSlot

    when (slot) {
      10 -> { // 表示モード
        data.mode =
            if (data.mode == DisplayMode.NORMAL) DisplayMode.END_ROLL else DisplayMode.NORMAL
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f)
        animationEngine.resetState(data.id)
      }
      12 -> { // 正面追従 (ビルボード)
        data.billboard =
            if (data.billboard == Display.Billboard.CENTER) Display.Billboard.FIXED
            else Display.Billboard.CENTER
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f)
      }
      14 -> { // 文字の向き (横書き/縦書き)
        data.orientation =
            if (data.orientation == TextOrientation.HORIZONTAL) TextOrientation.VERTICAL
            else TextOrientation.HORIZONTAL
        if (data.orientation == TextOrientation.VERTICAL && data.mode == DisplayMode.END_ROLL) {
          data.mode = DisplayMode.NORMAL
          player.sendMessage(TextFormatter.format("&e[ホログラム] &7縦書きモードのため、表示モードを通常表示に変更しました。"))
        }
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f)
        animationEngine.resetState(data.id)
      }
      16 -> { // 自動再生モード
        val next =
            when (data.playMode) {
              PlayMode.ALWAYS -> PlayMode.PROXIMITY
              PlayMode.PROXIMITY -> PlayMode.MANUAL
              PlayMode.MANUAL -> PlayMode.ALWAYS
            }
        data.playMode = next
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f)
      }
      22 -> { // テキスト行編集へ
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.4f)
        TextEditorGui.open(player, data)
        return
      }
      28 -> { // 再生範囲 (検知半径)
        val ranges = listOf(5.0, 10.0, 15.0, 20.0, 30.0)
        val curIdx = ranges.indexOf(data.proximityRange)
        val nextIdx = if (curIdx == -1 || curIdx + 1 >= ranges.size) 0 else curIdx + 1
        data.proximityRange = ranges[nextIdx]
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.1f)
      }
      30 -> { // 文字サイズ (スケール)
        val scales = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
        val curIdx = scales.indexOf(data.scale)
        val nextIdx = if (curIdx == -1 || curIdx + 1 >= scales.size) 0 else curIdx + 1
        data.scale = scales[nextIdx]
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.1f)
      }
      32 -> { // 表示行数 (ウィンドウ行数)
        val winSizes = listOf(1, 2, 3, 5, 8, 10)
        val curIdx = winSizes.indexOf(data.windowLines)
        val nextIdx = if (curIdx == -1 || curIdx + 1 >= winSizes.size) 0 else curIdx + 1
        data.windowLines = winSizes[nextIdx]
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.1f)
        animationEngine.resetState(data.id)
      }
      34 -> { // スクロール速度
        val speeds = listOf(80, 60, 40, 25, 15)
        val curIdx = speeds.indexOf(data.scrollSpeed)
        val nextIdx = if (curIdx == -1 || curIdx + 1 >= speeds.size) 0 else curIdx + 1
        data.scrollSpeed = speeds[nextIdx]
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.1f)
        animationEngine.resetState(data.id)
      }
      40 -> { // 背景スタイル
        val nextBg =
            when (data.backgroundType) {
              BackgroundType.TRANSPARENT -> BackgroundType.DEFAULT
              BackgroundType.DEFAULT -> BackgroundType.BLACK
              BackgroundType.BLACK -> BackgroundType.TRANSPARENT
            }
        data.backgroundType = nextBg
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.1f)
      }
      45 -> { // 閉じる
        player.closeInventory()
        return
      }
      53 -> { // 削除確認へ
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 0.5f)
        DeleteConfirmGui.open(player, data)
        return
      }
      else -> return
    }

    // 変更をエンティティに反映し、メニューを再描画
    manager.spawnOrUpdateEntities(data)
    manager.saveAll()
    MainMenuGui.open(player, data)
  }

  private fun handleTextEditorClick(
      event: InventoryClickEvent,
      player: Player,
      holder: TextEditorHolder,
  ) {
    event.isCancelled = true
    val data = manager.get(holder.hologramId) ?: return
    val slot = event.rawSlot

    when (slot) {
      in 0..35 -> {
        val index = holder.page * 36 + slot
        if (index in data.lines.indices) {
          if (event.click == ClickType.SHIFT_RIGHT) {
            // Shift+右クリックでその行を削除
            val removed = data.lines.removeAt(index)
            player.sendMessage(TextFormatter.format("&c[ホログラム] &7行を削除しました: &r$removed"))
            player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 0.8f, 1.2f)
            manager.spawnOrUpdateEntities(data)
            manager.saveAll()
            animationEngine.resetState(data.id)
            TextEditorGui.open(player, data, holder.page)
          } else if (event.click == ClickType.RIGHT) {
            // 通常右クリックでカラーパレットを開く！
            player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.4f)
            ColorPaletteGui.open(player, data, index)
          } else {
            // 左クリックでチャット編集
            chatInputHandler.expectInput(player, data.id, index)
          }
        }
      }
      45 -> { // メインメニューに戻る
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.0f)
        MainMenuGui.open(player, data)
      }
      48 -> { // 新規行追加
        chatInputHandler.expectInput(player, data.id, -1)
      }
      50 -> { // 末尾の行を削除
        if (data.lines.isNotEmpty()) {
          val removed = data.lines.removeAt(data.lines.size - 1)
          player.sendMessage(TextFormatter.format("&c[ホログラム] &7末尾の行を削除しました: &r$removed"))
          player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 0.8f, 1.2f)
          manager.spawnOrUpdateEntities(data)
          manager.saveAll()
          animationEngine.resetState(data.id)
          TextEditorGui.open(player, data, holder.page)
        }
      }
      53 -> { // 全行クリア
        if (data.lines.isNotEmpty()) {
          data.lines.clear()
          data.lines.add("&7(空のホログラム)")
          player.sendMessage(TextFormatter.format("&4[ホログラム] &7すべての行をクリアしました。"))
          player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f)
          manager.spawnOrUpdateEntities(data)
          manager.saveAll()
          animationEngine.resetState(data.id)
          TextEditorGui.open(player, data, holder.page)
        }
      }
    }
  }

  private fun handleColorPaletteClick(
      event: InventoryClickEvent,
      player: Player,
      holder: ColorPaletteHolder,
  ) {
    event.isCancelled = true
    val data = manager.get(holder.hologramId) ?: return
    val lineIdx = holder.lineIndex
    if (lineIdx !in data.lines.indices) {
      ColorPaletteGui.clearGradientSelections(player.uniqueId)
      TextEditorGui.open(player, data)
      return
    }

    val slot = event.rawSlot

    when (slot) {
      in 0..15 -> {
        val entry = ColorPaletteGui.colorEntries[slot]
        val plainText = TextFormatter.stripColorCodes(data.lines[lineIdx])

        if (event.isRightClick) {
          // 右クリック: グラデーション用の色を追加（最大5色）
          val added = ColorPaletteGui.addGradientColor(player.uniqueId, entry.hexColor)
          val count = ColorPaletteGui.getSelectionCount(player.uniqueId)
          if (added) {
            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_CHIME,
                1.0f,
                1.0f + count * 0.1f,
            )
            if (count >= 2) {
              player.sendMessage(
                  TextFormatter.format(
                      "&b[カラーパレット] &e${entry.name}&e を追加 (${count}/5色)。&aスロット27で適用できます！"
                  )
              )
            } else {
              player.sendMessage(
                  TextFormatter.format(
                      "&b[カラーパレット] &e${entry.name}&e を1色目として選択。あと1色以上追加してから適用ボタンを押してください。"
                  )
              )
            }
          } else {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 0.8f, 1.2f)
            player.sendMessage(TextFormatter.format("&c[カラーパレット] 既に5色選択済みです。リセットしてから選び直してください。"))
          }
          ColorPaletteGui.open(player, data, lineIdx)
          return
        } else {
          // 左クリック: 単色を直接適用
          ColorPaletteGui.clearGradientSelections(player.uniqueId)
          data.lines[lineIdx] = "${entry.colorCode}$plainText"
          player.playSound(player.location, Sound.ITEM_DYE_USE, 1.0f, 1.2f)
        }

        manager.spawnOrUpdateEntities(data)
        manager.saveAll()
        animationEngine.resetState(data.id)
        TextEditorGui.open(player, data)
      }
      16 -> {
        // レインボー
        ColorPaletteGui.clearGradientSelections(player.uniqueId)
        val plainText = TextFormatter.stripColorCodes(data.lines[lineIdx])
        data.lines[lineIdx] = "<rainbow>$plainText</rainbow>"

        player.playSound(player.location, Sound.ITEM_DYE_USE, 1.0f, 1.5f)
        manager.spawnOrUpdateEntities(data)
        manager.saveAll()
        animationEngine.resetState(data.id)
        TextEditorGui.open(player, data)
      }
      17 -> {
        // 夕焼けグラデーション (プリセット)
        ColorPaletteGui.clearGradientSelections(player.uniqueId)
        val plainText = TextFormatter.stripColorCodes(data.lines[lineIdx])
        data.lines[lineIdx] = "<gradient:#FF512F:#DD2476>$plainText</gradient>"

        player.playSound(player.location, Sound.ITEM_DYE_USE, 1.0f, 1.4f)
        manager.spawnOrUpdateEntities(data)
        manager.saveAll()
        animationEngine.resetState(data.id)
        TextEditorGui.open(player, data)
      }
      27 -> {
        // グラデーション適用ボタン
        val count = ColorPaletteGui.getSelectionCount(player.uniqueId)
        if (count < 2) {
          player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f)
          player.sendMessage(TextFormatter.format("&c[カラーパレット] 2色以上選択してから適用してください。(現在${count}色)"))
          return
        }
        val plainText = TextFormatter.stripColorCodes(data.lines[lineIdx])
        data.lines[lineIdx] = ColorPaletteGui.buildGradientTag(player.uniqueId, plainText)
        ColorPaletteGui.clearGradientSelections(player.uniqueId)

        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f)
        player.sendMessage(TextFormatter.format("&a[カラーパレット] &f${count}色グラデーションを適用しました！"))
        manager.spawnOrUpdateEntities(data)
        manager.saveAll()
        animationEngine.resetState(data.id)
        TextEditorGui.open(player, data)
      }
      28 -> {
        // グラデーション選択リセット
        ColorPaletteGui.clearGradientSelections(player.uniqueId)
        player.playSound(player.location, Sound.ITEM_BUCKET_EMPTY, 0.8f, 1.4f)
        player.sendMessage(TextFormatter.format("&7[カラーパレット] グラデーションの色選択をリセットしました。"))
        ColorPaletteGui.open(player, data, lineIdx)
      }
      29 -> {
        // 戻る
        ColorPaletteGui.clearGradientSelections(player.uniqueId)
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.0f)
        TextEditorGui.open(player, data)
      }
      31 -> {
        // 太字トグル
        val current = data.lines[lineIdx]
        if (current.contains("&l") || current.contains("<b>")) {
          data.lines[lineIdx] = current.replace("&l", "").replace("<b>", "").replace("</b>", "")
        } else {
          data.lines[lineIdx] = "&l$current"
        }

        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.5f)
        manager.spawnOrUpdateEntities(data)
        manager.saveAll()
        animationEngine.resetState(data.id)
        ColorPaletteGui.open(player, data, lineIdx)
      }
      32 -> {
        // 装飾リセット
        ColorPaletteGui.clearGradientSelections(player.uniqueId)
        val plainText = TextFormatter.stripColorCodes(data.lines[lineIdx])
        data.lines[lineIdx] = plainText

        player.playSound(player.location, Sound.ITEM_BUCKET_EMPTY, 0.8f, 1.2f)
        manager.spawnOrUpdateEntities(data)
        manager.saveAll()
        animationEngine.resetState(data.id)
        TextEditorGui.open(player, data)
      }
      35 -> {
        // チャット入力へ切り替え
        ColorPaletteGui.clearGradientSelections(player.uniqueId)
        chatInputHandler.expectInput(player, data.id, lineIdx)
      }
    }
  }

  private fun handleDeleteConfirmClick(
      event: InventoryClickEvent,
      player: Player,
      holder: DeleteConfirmHolder,
  ) {
    event.isCancelled = true
    val slot = event.rawSlot
    val data = manager.get(holder.hologramId)

    when (slot) {
      11 -> { // 削除実行
        manager.removeHologram(holder.hologramId)
        player.closeInventory()
        player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f)
        player.sendMessage(TextFormatter.format("&c[ホログラム] &7ホログラムを撤去・削除しました。"))
      }
      15 -> { // キャンセル
        player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.0f)
        if (data != null) {
          MainMenuGui.open(player, data)
        } else {
          player.closeInventory()
        }
      }
    }
  }
}
