package icu.oyasai.utilities.hologram.util

import java.util.regex.Pattern
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

object TextFormatter {
  private val miniMessage = MiniMessage.miniMessage()
  private val legacySerializer =
      LegacyComponentSerializer.builder()
          .character('&')
          .hexColors()
          .useUnusualXRepeatedCharacterHexFormat()
          .build()

  private val sectionSerializer =
      LegacyComponentSerializer.builder()
          .character('§')
          .hexColors()
          .useUnusualXRepeatedCharacterHexFormat()
          .build()

  private val hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})")

  /** &a や &#RRGGBB、<red> などの MiniMessage / レガシー表記を Component に変換します。 && でリテラルの & をエスケープできます。 */
  fun format(input: String): Component {
    // && を一時的なプレースホルダーに退避
    val escaped = input.replace("&&", "\uE000")

    // &#RRGGBB を <#RRGGBB> に変換
    var converted = hexPattern.matcher(escaped).replaceAll("<#$1>")

    // § が含まれている場合は & に統一
    if (converted.contains("§")) {
      converted = sectionSerializer.serialize(sectionSerializer.deserialize(converted))
    }

    val component =
        if (converted.contains("<") && converted.contains(">")) {
          try {
            val legacyConverted = legacySerializer.deserialize(converted)
            val intermediate = legacySerializer.serialize(legacyConverted)
            miniMessage.deserialize(intermediate)
          } catch (_: Exception) {
            legacySerializer.deserialize(converted)
          }
        } else {
          legacySerializer.deserialize(converted)
        }

    // プレースホルダーをリテラル & に戻す
    return component.replaceText { builder -> builder.matchLiteral("\uE000").replacement("&") }
  }

  /** 複数行のテキストを日本の伝統的な縦書き（上から下、右から左）に変換します。 列間は "&r　" のみで分離し、トークン末尾の&rをなくすことで不可視文字を最小化します。 */
  fun verticalize(lines: List<String>): List<String> {
    if (lines.isEmpty()) return emptyList()

    // 各行を色付き文字トークンに分解
    val tokenizedLines = lines.map { splitIntoCharsWithIsolatedColor(toFullWidth(it)) }
    val maxLen = tokenizedLines.maxOfOrNull { it.size } ?: 0
    if (maxLen == 0) return emptyList()

    if (tokenizedLines.size == 1) {
      return tokenizedLines[0]
    }

    val result = mutableListOf<String>()
    for (row in 0 until maxLen) {
      val parts = mutableListOf<String>()
      // 日本語の縦書きは「右から左」なので逆順（最後の行から最初の行）で配置
      for (col in tokenizedLines.indices.reversed()) {
        val colTokens = tokenizedLines[col]
        if (row < colTokens.size) {
          parts.add(colTokens[row])
        } else {
          parts.add("　") // パディング（全角スペース）
        }
      }
      // 列間を "&r　"（リセット+全角スペース）で結合し、先頭に &r を付与
      // → 各列の色が隣の列へ漏れない
      result.add("&r" + parts.joinToString("&r　"))
    }
    return result
  }

  /** 半角英数字や記号を全角文字に変換します（カラーコードやタグ部分は保護）。 */
  fun toFullWidth(text: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
      // Hexコード &#RRGGBB の保護
      if (text.startsWith("&#", i) && i + 8 <= text.length) {
        val hexMatch = text.substring(i, i + 8)
        if (hexMatch.matches(Regex("&#[0-9a-fA-F]{6}"))) {
          sb.append(hexMatch)
          i += 8
          continue
        }
      }
      // レガシーカラーコード &a / §a の保護
      if ((text[i] == '&' || text[i] == '§') && i + 1 < text.length) {
        val code = text.substring(i, i + 2)
        if (code.matches(Regex("[&§][0-9a-fk-orA-FK-OR]"))) {
          sb.append(code)
          i += 2
          continue
        }
      }

      val c = text[i]
      when (c) {
        ' ' -> sb.append('　')
        in '!'..'~' -> sb.append((c.code + 0xFEE0).toChar())
        else -> sb.append(c)
      }
      i++
    }
    return sb.toString()
  }

  /** 文字列を1文字ごとのリストに分割し、直前のカラー装飾を各文字に付与します。 色が指定されていない文字には明示的に &r を付与して他行・他文字への色漏れを防ぎます。 */
  private fun splitIntoCharsWithIsolatedColor(text: String): List<String> {
    val result = mutableListOf<String>()
    var currentColor = ""
    var i = 0

    while (i < text.length) {
      // Hexコード &#RRGGBB のチェック
      if (text.startsWith("&#", i) && i + 8 <= text.length) {
        val hexMatch = text.substring(i, i + 8)
        if (hexMatch.matches(Regex("&#[0-9a-fA-F]{6}"))) {
          currentColor = hexMatch
          i += 8
          continue
        }
      }
      // 通常のレガシーカラー &a または §a のチェック
      if ((text[i] == '&' || text[i] == '§') && i + 1 < text.length) {
        val code = text.substring(i, i + 2)
        if (code.matches(Regex("[&§][0-9a-fk-orA-FK-OR]"))) {
          val charCode = code[1]
          if (charCode == 'r' || charCode == 'R') {
            currentColor = ""
          } else {
            currentColor += "&$charCode"
          }
          i += 2
          continue
        }
      }

      // 通常文字: 色があればその色、なければ &r を先頭に付与（末尾の &r は付けない）
      val charStr = text[i].toString()
      if (currentColor.isNotEmpty()) {
        result.add("$currentColor$charStr")
      } else {
        result.add("&r$charStr")
      }
      i++
    }

    return result
  }

  /** 文字列からレガシーカラーコード (&a, §a), Hexコード (&#RRGGBB, <#RRGGBB>), MiniMessageタグを除去し、純粋な本文のみを抽出します。 */
  fun stripColorCodes(text: String): String {
    var s = text
    s = s.replace(Regex("&#[0-9a-fA-F]{6}"), "")
    s = s.replace(Regex("<#[0-9a-fA-F]{6}>"), "")
    s = s.replace(Regex("<[/]?[a-zA-Z_0-9:]+>"), "")
    s = s.replace(Regex("[&§][0-9a-fk-orA-FK-OR]"), "")
    return s.trim()
  }

  /** 複数行のテキストを結合する際、行を跨ぐ色漏れを防ぐために各行を &r で分離して結合します。 */
  fun joinLinesSafely(lines: List<String>): String {
    return lines.joinToString("\n") { line -> "&r$line&r" }
  }
}
