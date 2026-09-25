package icu.oyasai.utilities.hologram.gui

import icu.oyasai.utilities.hologram.model.HologramData
import icu.oyasai.utilities.hologram.util.ItemBuilder
import icu.oyasai.utilities.hologram.util.TextFormatter
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object TextEditorGui {
  fun open(player: Player, data: HologramData, page: Int = 0) {
    val holder = TextEditorHolder(data.id, page)
    val inv =
        Bukkit.createInventory(
            holder,
            54,
            TextFormatter.format("&8テキスト行編集 (&f${data.lines.size}&8行)"),
        )
    holder.setInventory(inv)

    // 背景ガラス
    val filler = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build()
    for (i in 36 until 54) {
      inv.setItem(i, filler)
    }

    // スロット 0〜35 に行を表示
    val linesPerPage = 36
    val startIndex = page * linesPerPage
    val endIndex = (startIndex + linesPerPage).coerceAtMost(data.lines.size)

    for (i in startIndex until endIndex) {
      val slot = i - startIndex
      val rawLine = data.lines[i]
      val lineItem =
          ItemBuilder(Material.PAPER)
              .name("&e&l${i + 1} 行目: &r$rawLine")
              .lore(
                  "&7内容: &f$rawLine",
                  "",
                  "&e▶ 左クリック: チャットでテキストを編集",
                  "&d▶ 右クリック: 🎨 パレットから文字色を変更",
                  "&c▶ Shift+右クリック: この行を削除",
              )
              .build()
      inv.setItem(slot, lineItem)
    }

    // 45: メインメニューに戻る
    val backItem = ItemBuilder(Material.ARROW).name("&e◀ 設定メニューに戻る").build()
    inv.setItem(45, backItem)

    // 48: 新規行追加
    val addItem =
        ItemBuilder(Material.EMERALD)
            .name("&a&l＋ 新しい行を追加")
            .lore("&7クリックするとチャット入力モードになり、", "&7末尾に新しいテキスト行を追加できます。")
            .build()
    inv.setItem(48, addItem)

    // 50: 最後の行を削除
    if (data.lines.isNotEmpty()) {
      val popItem = ItemBuilder(Material.HOPPER).name("&c&l末尾の行を削除").lore("&7一番最後の行を削除します。").build()
      inv.setItem(50, popItem)
    }

    // 53: 全行クリア
    if (data.lines.isNotEmpty()) {
      val clearItem =
          ItemBuilder(Material.TNT).name("&4&lすべての行をクリア").lore("&c登録されている行をすべて消去します。").build()
      inv.setItem(53, clearItem)
    }

    player.openInventory(inv)
  }
}
