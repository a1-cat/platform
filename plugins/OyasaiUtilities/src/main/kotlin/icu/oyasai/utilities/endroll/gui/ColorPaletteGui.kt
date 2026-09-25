package icu.oyasai.utilities.endroll.gui

import icu.oyasai.utilities.endroll.model.HologramData
import icu.oyasai.utilities.endroll.util.ItemBuilder
import icu.oyasai.utilities.endroll.util.TextFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object ColorPaletteGui {

  data class ColorEntry(
      val material: Material,
      val colorCode: String,
      val hexColor: String,
      val name: String,
  )

  val colorEntries =
      listOf(
          ColorEntry(Material.WHITE_DYE, "&f", "#FFFFFF", "&f白色 (White)"),
          ColorEntry(Material.LIGHT_GRAY_DYE, "&7", "#AAAAAA", "&7明灰色 (Light Gray)"),
          ColorEntry(Material.GRAY_DYE, "&8", "#555555", "&8濃灰色 (Dark Gray)"),
          ColorEntry(Material.BLACK_DYE, "&0", "#000000", "&0黒色 (Black)"),
          ColorEntry(Material.RED_DYE, "&c", "#FF5555", "&c赤色 (Red)"),
          ColorEntry(Material.ORANGE_DYE, "&6", "#FFAA00", "&6橙色 (Orange)"),
          ColorEntry(Material.YELLOW_DYE, "&e", "#FFFF55", "&e黄色 (Yellow)"),
          ColorEntry(Material.LIME_DYE, "&a", "#55FF55", "&a黄緑色 (Lime)"),
          ColorEntry(Material.GREEN_DYE, "&2", "#00AA00", "&2深緑色 (Green)"),
          ColorEntry(Material.CYAN_DYE, "&3", "#00AAAA", "&3青緑色 (Cyan)"),
          ColorEntry(Material.LIGHT_BLUE_DYE, "&b", "#55FFFF", "&b空色 (Aqua)"),
          ColorEntry(Material.BLUE_DYE, "&9", "#5555FF", "&9青色 (Blue)"),
          ColorEntry(Material.PURPLE_DYE, "&5", "#AA00AA", "&5紫色 (Purple)"),
          ColorEntry(Material.MAGENTA_DYE, "&d", "#FF55FF", "&dピンク紫 (Magenta)"),
          ColorEntry(Material.PINK_DYE, "&#FFB6C1", "#FFB6C1", "<#FFB6C1>桜色 (Pink)"),
          ColorEntry(Material.GOLD_INGOT, "&6&l", "#FFD700", "&6&lゴールド (Gold Bold)"),
      )

  // プレイヤーごとのグラデーション選択色リスト（最大5色）
  val gradientSelections = ConcurrentHashMap<UUID, MutableList<String>>()

  /** グラデーション選択をリセット */
  fun clearGradientSelections(playerId: UUID) {
    gradientSelections.remove(playerId)
  }

  /** 選択済みの色数を取得 */
  fun getSelectionCount(playerId: UUID): Int {
    return gradientSelections[playerId]?.size ?: 0
  }

  /** 色を選択リストに追加（最大5色）。true=追加成功, false=上限に達している */
  fun addGradientColor(playerId: UUID, hexColor: String): Boolean {
    val list = gradientSelections.getOrPut(playerId) { mutableListOf() }
    if (list.size >= 5) return false
    list.add(hexColor)
    return true
  }

  /** 選択済みの色リストからグラデーション MiniMessage タグを生成 */
  fun buildGradientTag(playerId: UUID, plainText: String): String {
    val list = gradientSelections[playerId] ?: return plainText
    return when (list.size) {
      0 -> plainText
      1 -> "${list[0]}$plainText" // 単色（本来このルートは通らないはず）
      else -> {
        val stops = list.joinToString(":")
        "<gradient:$stops>$plainText</gradient>"
      }
    }
  }

  fun open(player: Player, data: HologramData, lineIndex: Int) {
    val holder = ColorPaletteHolder(data.id, lineIndex)
    val inv =
        Bukkit.createInventory(
            holder,
            36,
            TextFormatter.format("&8文字色・グラデ選択: &e${lineIndex + 1}&8行目"),
        )
    holder.setInventory(inv)

    val filler = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build()
    for (i in 0 until 36) {
      inv.setItem(i, filler)
    }

    val selectedList = gradientSelections[player.uniqueId] ?: emptyList()
    val selectedCount = selectedList.size

    // スロット 0〜15 に染料を配置
    colorEntries.forEachIndexed { i, entry ->
      val isInSelection = selectedList.contains(entry.hexColor)
      val selIdx = selectedList.indexOf(entry.hexColor)
      val orderStr = if (selIdx >= 0) "&a[${ selIdx + 1}色目として選択中]" else ""

      val loreLines = mutableListOf<String>()
      loreLines.add("&7左クリック: &fこの単色を直接適用")
      if (selectedCount < 5) {
        loreLines.add("&d右クリック: &eグラデーションの色として追加 (${selectedCount}/5)")
      } else {
        loreLines.add("&8右クリック: 既に5色選択済み（これ以上追加不可）")
      }
      if (isInSelection) {
        loreLines.add(orderStr)
      }
      if (selectedCount >= 2) {
        loreLines.add("")
        loreLines.add("&a▶ スロット27の「グラデ適用」ボタンで確定！")
      }

      val item =
          ItemBuilder(entry.material)
              .name(entry.name)
              .lore(*loreLines.toTypedArray())
              .glow(isInSelection)
              .build()
      inv.setItem(i, item)
    }

    // 16: レインボー
    val rainbowItem =
        ItemBuilder(Material.NETHER_STAR)
            .name("<rainbow>虹色グラデーション</rainbow>")
            .lore("&7MiniMessageの虹色グラデーションを適用します。", "", "&e▶ 左クリックで適用")
            .build()
    inv.setItem(16, rainbowItem)

    // 17: 夕焼けグラデーション
    val sunsetItem =
        ItemBuilder(Material.BLAZE_POWDER)
            .name("<gradient:#FF512F:#DD2476>夕焼けグラデーション</gradient>")
            .lore("&7赤〜ピンクのプリセットグラデーションを適用します。", "", "&e▶ 左クリックで適用")
            .build()
    inv.setItem(17, sunsetItem)

    // 27: グラデーション適用ボタン（2色以上選択時のみ光る）
    val canApplyGrad = selectedCount >= 2
    val gradApplyItem =
        if (canApplyGrad) {
          val preview = selectedList.joinToString(" → ") { it }
          ItemBuilder(Material.PRISMARINE_CRYSTALS)
              .name("<gradient:${selectedList.joinToString(":")}>グラデーションを適用！</gradient>")
              .lore(
                  "&7選択した色: &f$preview",
                  "&7(${selectedCount}色グラデーション)",
                  "",
                  "&a▶ 左クリックで適用",
              )
              .glow(true)
              .build()
        } else {
          ItemBuilder(Material.PRISMARINE_CRYSTALS)
              .name("&7グラデーションを適用")
              .lore(
                  "&7染料を右クリックして色を選択 (2〜5色)",
                  "&7選択後にここをクリックして適用します。",
                  "",
                  if (selectedCount == 0) "&8現在: 色が選択されていません"
                  else "&e現在: ${selectedCount}色選択中 (あと${2 - selectedCount}色以上必要)",
              )
              .build()
        }
    inv.setItem(27, gradApplyItem)

    // 28: 選択リセット
    val resetGradItem =
        ItemBuilder(Material.BARRIER)
            .name(if (selectedCount > 0) "&cグラデーション選択をリセット (&f${selectedCount}色)" else "&8選択中の色なし")
            .lore("&7追加した色の選択をすべてクリアします。")
            .build()
    inv.setItem(28, resetGradItem)

    // 29: 戻る
    val backItem = ItemBuilder(Material.ARROW).name("&e◀ テキスト行一覧に戻る").build()
    inv.setItem(29, backItem)

    // 31: 太字追加 (&l)
    val boldItem =
        ItemBuilder(Material.ANVIL)
            .name("&f&l太字 (Bold) をトグル")
            .lore("&7現在の色を維持したまま、太字をON/OFFします。", "", "&e▶ 左クリックで太字切り替え")
            .build()
    inv.setItem(31, boldItem)

    // 32: 装飾クリア
    val clearItem =
        ItemBuilder(Material.MILK_BUCKET)
            .name("&f装飾・色をリセット")
            .lore("&7色や太字をすべてリセットして標準の白に戻します。", "", "&e▶ 左クリックでリセット")
            .build()
    inv.setItem(32, clearItem)

    // 35: チャットで詳細編集
    val chatItem =
        ItemBuilder(Material.WRITABLE_BOOK)
            .name("&bチャットで手動入力モードへ")
            .lore(
                "&7カラーコード(&e&&a&7や&e&&#RRGGBB&7)を自分で直接",
                "&7打ち込んで編集したい場合はこちら。",
                "",
                "&e▶ 左クリックしてチャット入力へ",
            )
            .build()
    inv.setItem(35, chatItem)

    player.openInventory(inv)
  }
}
