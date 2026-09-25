package icu.oyasai.utilities.hologram.gui

import icu.oyasai.utilities.hologram.model.HologramData
import icu.oyasai.utilities.hologram.util.ItemBuilder
import icu.oyasai.utilities.hologram.util.TextFormatter
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

object DeleteConfirmGui {
  fun open(player: Player, data: HologramData) {
    val holder = DeleteConfirmHolder(data.id)
    val inv = Bukkit.createInventory(holder, 27, TextFormatter.format("&4ホログラム削除の確認"))
    holder.setInventory(inv)

    // 背景ガラス
    val filler = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build()
    for (i in 0 until 27) {
      inv.setItem(i, filler)
    }

    // 11: 削除実行
    val deleteItem =
        ItemBuilder(Material.RED_CONCRETE)
            .name("&c&l本当に削除する")
            .lore("&7このホログラムをワールドから完全に撤去します。", "&c※ この操作は取り消せません。")
            .build()
    inv.setItem(11, deleteItem)

    // 15: キャンセル
    val cancelItem =
        ItemBuilder(Material.LIME_CONCRETE).name("&a&lキャンセルして戻る").lore("&7設定メニューに戻ります。").build()
    inv.setItem(15, cancelItem)

    player.openInventory(inv)
  }
}
