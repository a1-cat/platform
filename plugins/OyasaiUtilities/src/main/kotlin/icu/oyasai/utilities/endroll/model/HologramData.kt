package icu.oyasai.utilities.endroll.model

import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Display

enum class DisplayMode(val displayName: String) {
  NORMAL("通常表示"),
  END_ROLL("エンドロール"),
}

enum class TextOrientation(val displayName: String) {
  HORIZONTAL("横書き"),
  VERTICAL("縦書き (上から下)"),
}

enum class PlayMode(val displayName: String) {
  ALWAYS("常時再生"),
  PROXIMITY("プレイヤー接近時のみ"),
  MANUAL("手動再生"),
}

enum class BackgroundType(val displayName: String, val color: Color?) {
  DEFAULT("標準 (半透明黒)", null),
  TRANSPARENT("透明", Color.fromARGB(0, 0, 0, 0)),
  BLACK("完全な黒", Color.fromARGB(255, 0, 0, 0)),
}

data class HologramData(
    val id: UUID = UUID.randomUUID(),
    val owner: UUID,
    var location: Location,
    val lines: MutableList<String> = mutableListOf("&e&l新規ホログラム", "&7Shift+右クリックで設定を開く"),
    var mode: DisplayMode = DisplayMode.NORMAL,
    var billboard: Display.Billboard = Display.Billboard.CENTER,
    var orientation: TextOrientation = TextOrientation.HORIZONTAL,
    var playMode: PlayMode = PlayMode.PROXIMITY,
    var windowLines: Int = 5,
    var scrollSpeed: Int = 40, // ticks per line scroll
    var proximityRange: Double = 10.0,
    var scale: Float = 1.0f,
    var backgroundType: BackgroundType = BackgroundType.TRANSPARENT,
    var hasShadow: Boolean = true,
    var isPlaying: Boolean = true,
) {
  fun serialize(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    map["id"] = id.toString()
    map["owner"] = owner.toString()
    map["world"] = location.world?.name ?: "world"
    map["x"] = location.x
    map["y"] = location.y
    map["z"] = location.z
    map["yaw"] = location.yaw.toDouble()
    map["pitch"] = location.pitch.toDouble()
    map["lines"] = lines
    map["mode"] = mode.name
    map["billboard"] = billboard.name
    map["orientation"] = orientation.name
    map["playMode"] = playMode.name
    map["windowLines"] = windowLines
    map["scrollSpeed"] = scrollSpeed
    map["proximityRange"] = proximityRange
    map["scale"] = scale.toDouble()
    map["backgroundType"] = backgroundType.name
    map["hasShadow"] = hasShadow
    map["isPlaying"] = isPlaying
    return map
  }

  companion object {
    fun deserialize(map: Map<String, Any>): HologramData? {
      return try {
        val id = UUID.fromString(map["id"] as String)
        val owner = UUID.fromString(map["owner"] as String)
        val worldName = map["world"] as String
        val world = Bukkit.getWorld(worldName) ?: Bukkit.getWorlds().firstOrNull() ?: return null
        val x = (map["x"] as Number).toDouble()
        val y = (map["y"] as Number).toDouble()
        val z = (map["z"] as Number).toDouble()
        val yaw = (map["yaw"] as Number).toFloat()
        val pitch = (map["pitch"] as Number).toFloat()
        val location = Location(world, x, y, z, yaw, pitch)

        @Suppress("UNCHECKED_CAST")
        val lines =
            (map["lines"] as? List<String>)?.toMutableList() ?: mutableListOf("Hello Hologram")
        val mode =
            runCatching { DisplayMode.valueOf(map["mode"] as String) }
                .getOrDefault(DisplayMode.NORMAL)
        val billboard =
            runCatching { Display.Billboard.valueOf(map["billboard"] as String) }
                .getOrDefault(Display.Billboard.CENTER)
        val orientation =
            runCatching { TextOrientation.valueOf(map["orientation"] as String) }
                .getOrDefault(TextOrientation.HORIZONTAL)
        val playMode =
            runCatching { PlayMode.valueOf(map["playMode"] as String) }
                .getOrDefault(PlayMode.PROXIMITY)
        val windowLines = (map["windowLines"] as? Number)?.toInt() ?: 5
        val scrollSpeed = (map["scrollSpeed"] as? Number)?.toInt() ?: 40
        val proximityRange = (map["proximityRange"] as? Number)?.toDouble() ?: 10.0
        val scale = (map["scale"] as? Number)?.toFloat() ?: 1.0f
        val backgroundType =
            runCatching { BackgroundType.valueOf(map["backgroundType"] as String) }
                .getOrDefault(BackgroundType.TRANSPARENT)
        val hasShadow = (map["hasShadow"] as? Boolean) ?: true
        val isPlaying = (map["isPlaying"] as? Boolean) ?: true

        HologramData(
            id = id,
            owner = owner,
            location = location,
            lines = lines,
            mode = mode,
            billboard = billboard,
            orientation = orientation,
            playMode = playMode,
            windowLines = windowLines,
            scrollSpeed = scrollSpeed,
            proximityRange = proximityRange,
            scale = scale,
            backgroundType = backgroundType,
            hasShadow = hasShadow,
            isPlaying = isPlaying,
        )
      } catch (e: Exception) {
        Bukkit.getLogger().warning("[EndRollHologram] ホログラムデータの読み込みに失敗しました: ${e.message}")
        null
      }
    }
  }
}
