package icu.oyasai.utilities.hologram.manager

import icu.oyasai.utilities.hologram.model.BackgroundType
import icu.oyasai.utilities.hologram.model.DisplayMode
import icu.oyasai.utilities.hologram.model.HologramData
import icu.oyasai.utilities.hologram.model.TextOrientation
import icu.oyasai.utilities.hologram.storage.YamlStorage
import icu.oyasai.utilities.hologram.util.TextFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

class HologramManager(private val plugin: JavaPlugin) {
  private val storage = YamlStorage(plugin)
  private val holograms = ConcurrentHashMap<UUID, HologramData>()
  private val activeDisplays = ConcurrentHashMap<UUID, TextDisplay>()
  private val activeInteractions = ConcurrentHashMap<UUID, Interaction>()

  val holoIdKey = NamespacedKey(plugin, "hologram_id")

  fun loadAll() {
    val loaded = storage.loadAll()
    holograms.putAll(loaded)
    plugin.logger.info("${holograms.size} 件のホログラムデータを読み込みました。")

    // 既存のエンティティをスポーンまたは同期
    Bukkit.getScheduler()
        .runTask(
            plugin,
            Runnable { holograms.values.forEach { data -> spawnOrUpdateEntities(data) } },
        )
  }

  fun saveAll() {
    storage.saveAll(holograms.values)
  }

  fun getAll(): Collection<HologramData> = holograms.values

  fun get(id: UUID): HologramData? = holograms[id]

  fun getPlayerHolograms(owner: UUID): List<HologramData> {
    return holograms.values.filter { it.owner == owner }
  }

  /** 新しいホログラムを作成・スポーンします。 */
  fun createHologram(owner: UUID, location: Location): HologramData {
    val data = HologramData(owner = owner, location = location)
    holograms[data.id] = data
    spawnOrUpdateEntities(data)
    saveAll()
    return data
  }

  /** ホログラムを削除し、エンティティを除去します。 */
  fun removeHologram(id: UUID) {
    holograms.remove(id)
    activeDisplays.remove(id)?.remove()
    activeInteractions.remove(id)?.remove()
    saveAll()
  }

  /** ホログラムのエンティティ（TextDisplay + Interaction）をスポーンまたはプロパティ更新します。 */
  fun spawnOrUpdateEntities(data: HologramData) {
    val loc = data.location
    val world = loc.world ?: return
    if (!world.isChunkLoaded(loc.blockX shr 4, loc.blockZ shr 4)) return

    // 既存のTextDisplayを確認、なければスポーン
    var display = activeDisplays[data.id]
    if (display == null || !display.isValid) {
      // ワールド内の既存エンティティからPDCで探す（サーバー再起動時対応）
      val existing =
          world.getNearbyEntities(loc, 2.0, 2.0, 2.0).firstOrNull {
            it is TextDisplay &&
                it.persistentDataContainer.get(holoIdKey, PersistentDataType.STRING) ==
                    data.id.toString()
          } as? TextDisplay

      display = existing ?: (world.spawnEntity(loc, EntityType.TEXT_DISPLAY) as TextDisplay)
      display.persistentDataContainer.set(holoIdKey, PersistentDataType.STRING, data.id.toString())
      activeDisplays[data.id] = display
    }

    // 既存のInteraction（クリック検知用）を確認
    var interaction = activeInteractions[data.id]
    if (interaction == null || !interaction.isValid) {
      val existing =
          world.getNearbyEntities(loc, 2.0, 2.0, 2.0).firstOrNull {
            it is Interaction &&
                it.persistentDataContainer.get(holoIdKey, PersistentDataType.STRING) ==
                    data.id.toString()
          } as? Interaction

      interaction = existing ?: (world.spawnEntity(loc, EntityType.INTERACTION) as Interaction)
      interaction.persistentDataContainer.set(
          holoIdKey,
          PersistentDataType.STRING,
          data.id.toString(),
      )
      interaction.interactionWidth = 2.0f
      interaction.interactionHeight = 2.0f
      interaction.isResponsive = true
      activeInteractions[data.id] = interaction
    }

    // TextDisplayのプロパティを更新
    applyProperties(data, display)
  }

  /** TextDisplayに設定データを反映します。 */
  fun applyProperties(data: HologramData, display: TextDisplay) {
    display.billboard = data.billboard
    display.isShadowed = data.hasShadow
    display.isSeeThrough = false

    // 背景色
    when (data.backgroundType) {
      BackgroundType.DEFAULT -> display.isDefaultBackground = true
      BackgroundType.TRANSPARENT -> {
        display.isDefaultBackground = false
        display.backgroundColor = data.backgroundType.color
      }
      BackgroundType.BLACK -> {
        display.isDefaultBackground = false
        display.backgroundColor = data.backgroundType.color
      }
    }

    // スケール
    val scaleVec = Vector3f(data.scale, data.scale, data.scale)
    val trans =
        Transformation(
            Vector3f(0f, 0f, 0f),
            AxisAngle4f(0f, 0f, 0f, 1f),
            scaleVec,
            AxisAngle4f(0f, 0f, 0f, 1f),
        )
    display.transformation = trans
    display.interpolationDuration = 0

    // 通常モードの場合は全行をフォーマットして表示
    if (data.mode == DisplayMode.NORMAL) {
      val displayLines =
          if (data.orientation == TextOrientation.VERTICAL) {
            TextFormatter.verticalize(data.lines)
          } else {
            data.lines
          }

      val joined = TextFormatter.joinLinesSafely(displayLines)
      display.text(TextFormatter.format(joined))
    }
  }

  fun getDisplay(id: UUID): TextDisplay? = activeDisplays[id]

  fun getInteraction(id: UUID): Interaction? = activeInteractions[id]

  /** プレイヤーの視線（RayTrace）または最寄りのホログラムを検索します。 */
  fun findTargetHologram(player: Player, maxDistance: Double = 6.0): HologramData? {
    val eye = player.eyeLocation
    val dir = eye.direction

    // 1. RayTraceでInteractionまたはTextDisplayを直接検知
    val rayTrace =
        player.world.rayTraceEntities(eye, dir, maxDistance) { entity ->
          entity.persistentDataContainer.has(holoIdKey, PersistentDataType.STRING)
        }

    if (rayTrace?.hitEntity != null) {
      val idStr =
          rayTrace.hitEntity!!.persistentDataContainer.get(holoIdKey, PersistentDataType.STRING)
      if (idStr != null) {
        val id = runCatching { UUID.fromString(idStr) }.getOrNull()
        if (id != null && holograms.containsKey(id)) {
          return holograms[id]
        }
      }
    }

    // 2. 視線円錐内の最も近いホログラムを検索
    var nearest: HologramData? = null
    var minAngle = 0.5 // コサイン値（視野角内）
    var minDist = maxDistance

    for (data in holograms.values) {
      if (data.location.world != player.world) continue
      val dist = player.location.distance(data.location)
      if (dist > maxDistance) continue

      val toHolo = data.location.clone().subtract(eye).toVector().normalize()
      val dot = dir.dot(toHolo)
      if (dot > minAngle && dist < minDist) {
        minDist = dist
        nearest = data
      }
    }

    return nearest
  }

  /** プラグイン終了時のクリーンアップ */
  fun cleanup() {
    saveAll()
    activeDisplays.values.forEach { if (it.isValid) it.remove() }
    activeInteractions.values.forEach { if (it.isValid) it.remove() }
    activeDisplays.clear()
    activeInteractions.clear()
  }
}
