package icu.oyasai.utilities.hologram.gui

import icu.oyasai.utilities.hologram.model.DisplayMode
import icu.oyasai.utilities.hologram.model.HologramData
import icu.oyasai.utilities.hologram.model.TextOrientation
import icu.oyasai.utilities.hologram.util.ItemBuilder
import icu.oyasai.utilities.hologram.util.TextFormatter
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Player

object MainMenuGui {
  fun open(player: Player, data: HologramData) {
    val holder = MainMenuHolder(data.id)
    val inv = Bukkit.createInventory(holder, 54, TextFormatter.format("&8ホログラム設定メニュー"))
    holder.setInventory(inv)

    // 背景ガラス
    val filler = ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build()
    for (i in 0 until 54) {
      inv.setItem(i, filler)
    }

    // 10: 表示モード（通常 ⇔ エンドロール）
    val isEndRoll = data.mode == DisplayMode.END_ROLL
    val modeItem =
        ItemBuilder(if (isEndRoll) Material.FILLED_MAP else Material.BOOK)
            .name(if (isEndRoll) "&b&l表示モード: &eエンドロール" else "&b&l表示モード: &a通常表示")
            .lore(
                "&7現在: &f${data.mode.displayName}",
                "&7下から上へと流れる映画のような演出か、",
                "&7固定で常に表示する看板かを切り替えます。",
                "",
                "&e▶ クリックで切り替え",
            )
            .glow(isEndRoll)
            .build()
    inv.setItem(10, modeItem)

    // 12: 正面追従 (ビルボード)
    val isCenter = data.billboard == Display.Billboard.CENTER
    val billboardItem =
        ItemBuilder(Material.COMPASS)
            .name(if (isCenter) "&d&l正面追従: &aON (プレイヤー追従)" else "&d&l正面追従: &cOFF (角度固定)")
            .lore("&7ON: どの角度から見ても文字が正面を向きます", "&7OFF: 設置時の角度で固定された立体看板になります", "", "&e▶ クリックで切り替え")
            .glow(isCenter)
            .build()
    inv.setItem(12, billboardItem)

    // 14: 文字の向き (横書き / 縦書き)
    val isVertical = data.orientation == TextOrientation.VERTICAL
    val orientationItem =
        ItemBuilder(Material.WRITABLE_BOOK)
            .name(if (isVertical) "&6&l文字の向き: &e縦書き (上から下)" else "&6&l文字の向き: &a横書き")
            .lore(
                "&7現在: &f${data.orientation.displayName}",
                "&7和風建築や短冊、看板などに適した縦書き表示。",
                if (isVertical) "&c※ 縦書き時はエンドロールは無効になります" else "",
                "",
                "&e▶ クリックで切り替え",
            )
            .glow(isVertical)
            .build()
    inv.setItem(14, orientationItem)

    // 16: 自動再生モード
    val playModeItem =
        ItemBuilder(Material.REPEATER)
            .name("&a&l自動再生モード: &f${data.playMode.displayName}")
            .lore(
                "&7常時再生: 常にスクロール演出を行います",
                "&7接近時のみ: 周囲にプレイヤーがいる時だけ再生(軽量)",
                "&7手動再生: クリック等で再生/停止を制御",
                "",
                "&e▶ クリックでモード切り替え",
            )
            .build()
    inv.setItem(16, playModeItem)

    // 22: テキスト行編集
    val textItem =
        ItemBuilder(Material.FEATHER)
            .name("&e&l✍ テキスト内容の編集")
            .lore(
                "&7行数: &f${data.lines.size} 行",
                "&7テキストの追加・編集・削除を行います。",
                "&7カラーコード(&e&&a&7)やHex(&e&&#RRGGBB&7)も使えます！",
                "",
                "&e▶ クリックしてテキスト編集画面を開く",
            )
            .glow(true)
            .build()
    inv.setItem(22, textItem)

    // 28: 再生範囲 (検知半径)
    val rangeItem =
        ItemBuilder(Material.ENDER_PEARL)
            .name("&3&l再生・検知範囲: &b${data.proximityRange.toInt()} m")
            .lore(
                "&7プレイヤーがこの範囲内に近づいたときに",
                "&7自動再生が開始されます。",
                "&7設定値: 5m → 10m → 15m → 20m → 30m",
                "",
                "&e▶ クリックで範囲を変更",
            )
            .build()
    inv.setItem(28, rangeItem)

    // 30: 文字サイズ（スケール）
    val scaleItem =
        ItemBuilder(Material.SLIME_BALL)
            .name("&a&l文字サイズ: &e${data.scale}x")
            .lore(
                "&7テキストの拡大・縮小倍率を変更します。",
                "&7設定値: 0.5x → 0.75x → 1.0x → 1.5x → 2.0x → 3.0x",
                "",
                "&e▶ クリックでサイズを変更",
            )
            .build()
    inv.setItem(30, scaleItem)

    // 32: 表示行数 (ウィンドウサイズ)
    val windowItem =
        ItemBuilder(Material.ITEM_FRAME)
            .name("&5&lエンドロール表示行数: &d${data.windowLines} 行")
            .lore(
                "&7エンドロール時に一度に枠内に表示される行数です。",
                "&7設定値: 1行 → 2行 → 3行 → 5行 → 8行 → 10行",
                "",
                "&e▶ クリックで行数を変更",
            )
            .build()
    inv.setItem(32, windowItem)

    // 34: スクロール速度
    val speedName =
        when (data.scrollSpeed) {
          in 70..100 -> "超低速 (約4秒/行)"
          in 50..69 -> "低速 (約3秒/行)"
          in 35..49 -> "普通 (約2秒/行)"
          in 20..34 -> "高速 (約1.2秒/行)"
          else -> "超高速 (約0.7秒/行)"
        }
    val speedItem =
        ItemBuilder(Material.CLOCK)
            .name("&e&lスクロール速度: &6$speedName")
            .lore(
                "&71行進むのに要する時間（滑らかさ）を設定します。",
                "&7設定値: 超低速 → 低速 → 普通 → 高速 → 超高速",
                "",
                "&e▶ クリックで速度を変更",
            )
            .build()
    inv.setItem(34, speedItem)

    // 40: 背景タイプ
    val bgLore =
        mutableListOf(
            "&7文字の後ろのプレート背景を設定します。",
            "&7透明 / 標準(半透明黒) / 完全な黒",
        )
    if (isEndRoll) {
      bgLore.add("")
      bgLore.add("&e※ エンドロール時は「透明」を推奨")
      bgLore.add("&7（行の長さにより背景幅が変動するため）")
    }
    bgLore.add("")
    bgLore.add("&e▶ クリックで背景を変更")

    val bgItem =
        ItemBuilder(Material.TINTED_GLASS)
            .name("&7&l背景スタイル: &f${data.backgroundType.displayName}")
            .lore(bgLore)
            .build()
    inv.setItem(40, bgItem)

    // 45: 閉じる
    val closeItem = ItemBuilder(Material.BARRIER).name("&c&l設定メニューを閉じる").build()
    inv.setItem(45, closeItem)

    // 53: 撤去・削除
    val deleteItem =
        ItemBuilder(Material.LAVA_BUCKET)
            .name("&4&l❌ このホログラムを撤去・削除")
            .lore("&cこのホログラムを完全に削除します。", "&7※ クリックすると確認画面が開きます。")
            .build()
    inv.setItem(53, deleteItem)

    player.openInventory(inv)
  }
}
