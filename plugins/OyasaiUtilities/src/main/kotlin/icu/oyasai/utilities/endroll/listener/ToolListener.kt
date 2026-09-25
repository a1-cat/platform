package icu.oyasai.utilities.endroll.listener

import icu.oyasai.utilities.endroll.gui.MainMenuGui
import icu.oyasai.utilities.endroll.manager.HologramManager
import icu.oyasai.utilities.endroll.model.HologramData
import icu.oyasai.utilities.endroll.model.ToolAdjustMode
import icu.oyasai.utilities.endroll.util.ItemBuilder
import icu.oyasai.utilities.endroll.util.TextFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class ToolListener(
    private val plugin: JavaPlugin,
    private val manager: HologramManager,
) : Listener {

  val toolKey = NamespacedKey(plugin, "is_hologram_tool")
  private val playerModes = ConcurrentHashMap<UUID, ToolAdjustMode>()
  private val playerYawSteps = ConcurrentHashMap<UUID, Float>()

  fun getYawStep(playerId: UUID): Float = playerYawSteps.getOrDefault(playerId, 15.0f)

  fun setYawStep(playerId: UUID, step: Float) {
    playerYawSteps[playerId] = step
  }

  fun getHologramTool(): ItemStack {
    return ItemBuilder(Material.AMETHYST_SHARD)
        .name("&d&lホロクリスタル")
        .lore(
            "&7Shift+左クリック: 設定メニューを開く / 新規設置",
            "&7Shift+右クリック(文字): 数値を減算",
            "",
            "&e文字を左クリック: 調整項目切替 (X→Y→Z→向き)",
            "&e文字を右クリック: 数値加算",
            "",
            "&8※回転の細かさは /erh angle <角度|reset> で変更可能",
            "&7※ホログラムに照準を合わせて操作してください",
        )
        .glow(true)
        .pdc(toolKey, "true")
        .build()
  }

  fun isHologramTool(item: ItemStack?): Boolean {
    if (item == null || item.type != Material.AMETHYST_SHARD) return false
    val meta = item.itemMeta ?: return false
    return meta.persistentDataContainer.has(toolKey, PersistentDataType.STRING)
  }

  /** テキストディスプレイ（Interaction）を左クリック（攻撃判定）した時の処理 */
  @EventHandler(priority = EventPriority.HIGH)
  fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
    val player = event.damager as? Player ?: return
    val entity = event.entity

    val holoId = getHoloIdFromEntity(entity) ?: return

    // ホログラムエンティティへの攻撃は常にキャンセル（殴り判定防止）
    event.isCancelled = true

    val item = player.inventory.itemInMainHand
    if (!isHologramTool(item)) return

    val data = manager.get(holoId) ?: return

    if (player.isSneaking) {
      // Shift+左クリック: メニューを開く
      openMenuIfPermitted(player, data)
    } else {
      // 通常左クリック: 調整項目の切り替え (X -> Y -> Z -> 向き -> X)
      switchAdjustMode(player)
    }
  }

  /** プレイヤーのブロック/空気に対するクリック操作 */
  @EventHandler(priority = EventPriority.HIGH)
  fun onPlayerInteract(event: PlayerInteractEvent) {
    if (event.hand != EquipmentSlot.HAND) return
    val player = event.player
    val item = player.inventory.itemInMainHand

    if (!isHologramTool(item)) return

    val action = event.action

    // ==========================================
    // 1. Shift + 左クリック: メニューを開く / 新規設置
    // ==========================================
    if (
        player.isSneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
    ) {
      event.isCancelled = true

      // まず視線先にホログラムがあればメニューを開く
      val targetHolo = manager.findTargetHologram(player, 6.0)
      if (targetHolo != null) {
        openMenuIfPermitted(player, targetHolo)
        return
      }

      // 視線先にホログラムがなく、ブロックを左クリックした場合 -> 新規設置
      if (action == Action.LEFT_CLICK_BLOCK && event.clickedBlock != null) {
        val clickedBlock = event.clickedBlock!!
        val face = event.blockFace

        val spawnLoc = clickedBlock.getRelative(face).location.add(0.5, 0.2, 0.5)
        spawnLoc.yaw = player.location.yaw
        spawnLoc.pitch = 0f

        val data = manager.createHologram(player.uniqueId, spawnLoc)
        player.playSound(player.location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f)
        player.sendMessage(TextFormatter.format("&a[ホログラム] &eホログラムを設置しました！設定メニューを開きます。"))

        MainMenuGui.open(player, data)
      }
      return
    }

    // ==========================================
    // 2. 通常左クリック (空気/ブロックを叩いた時、視線先にホログラムがあれば切替)
    // ==========================================
    if (
        !player.isSneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)
    ) {
      val targetHolo = manager.findTargetHologram(player, 6.0)
      if (targetHolo != null) {
        event.isCancelled = true
        switchAdjustMode(player)
        return
      }
    }

    // ==========================================
    // 3. Shift + 右クリック: 数値を減算 (視線先のホログラム)
    // ==========================================
    if (
        player.isSneaking &&
            (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
    ) {
      val targetHolo = manager.findTargetHologram(player, 6.0)
      if (targetHolo != null) {
        event.isCancelled = true
        adjustHologramValue(player, targetHolo, subtract = true)
        return
      }
    }

    // ==========================================
    // 4. 通常右クリック (視線先ホログラムへの数値加算)
    // ==========================================
    if (
        !player.isSneaking &&
            (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
    ) {
      val targetHolo = manager.findTargetHologram(player, 6.0)
      if (targetHolo != null) {
        event.isCancelled = true
        adjustHologramValue(player, targetHolo, subtract = false)
        return
      }
    }
  }

  /** ホログラムエンティティを右クリックした時の処理 */
  @EventHandler(priority = EventPriority.HIGH)
  fun onInteractEntity(event: PlayerInteractEntityEvent) {
    if (event.hand != EquipmentSlot.HAND) return
    val player = event.player
    val entity = event.rightClicked

    val holoId = getHoloIdFromEntity(entity) ?: return
    val data = manager.get(holoId) ?: return

    event.isCancelled = true

    if (!isHologramTool(player.inventory.itemInMainHand)) return

    if (player.isSneaking) {
      // Shift+右クリック: 数値を減算
      adjustHologramValue(player, data, subtract = true)
    } else {
      // 通常右クリック: 数値加算
      adjustHologramValue(player, data, subtract = false)
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  fun onInteractAtEntity(event: PlayerInteractAtEntityEvent) {
    if (event.hand != EquipmentSlot.HAND) return
    val player = event.player
    val entity = event.rightClicked

    val holoId = getHoloIdFromEntity(entity) ?: return
    val data = manager.get(holoId) ?: return

    event.isCancelled = true

    if (!isHologramTool(player.inventory.itemInMainHand)) return

    if (player.isSneaking) {
      adjustHologramValue(player, data, subtract = true)
    } else {
      adjustHologramValue(player, data, subtract = false)
    }
  }

  private fun getHoloIdFromEntity(entity: Entity): UUID? {
    val holoIdStr =
        entity.persistentDataContainer.get(manager.holoIdKey, PersistentDataType.STRING)
            ?: return null
    return runCatching { UUID.fromString(holoIdStr) }.getOrNull()
  }

  /** 調整項目の切り替え (X -> Y -> Z -> 向き -> X) */
  private fun switchAdjustMode(player: Player) {
    val current = playerModes.getOrDefault(player.uniqueId, ToolAdjustMode.X)
    val next = current.next()
    playerModes[player.uniqueId] = next

    val stepName =
        if (next == ToolAdjustMode.YAW) "${getYawStep(player.uniqueId)}°" else next.stepName

    player.playSound(player.location, Sound.UI_BUTTON_CLICK, 0.8f, 1.4f)
    player.sendActionBar(
        TextFormatter.format(
            "&b[ホロクリスタル] &7調整項目: &e&l${next.displayName} &7(右クリック: +$stepName / Shift+右クリック: -$stepName)"
        )
    )
  }

  /** ホログラムのエンティティそのものの座標または向きを動かします。 */
  private fun adjustHologramValue(player: Player, data: HologramData, subtract: Boolean) {
    if (data.owner != player.uniqueId && !player.hasPermission("endrollhologram.admin")) {
      player.sendMessage(TextFormatter.format("&c[ホログラム] &7他のプレイヤーが作成したホログラムは編集できません。"))
      return
    }

    val mode = playerModes.getOrDefault(player.uniqueId, ToolAdjustMode.X)
    val yawStep = getYawStep(player.uniqueId)
    val loc = data.location
    val sign = if (subtract) -1 else 1

    when (mode) {
      ToolAdjustMode.X -> loc.x += 0.1 * sign
      ToolAdjustMode.Y -> loc.y += 0.1 * sign
      ToolAdjustMode.Z -> loc.z += 0.1 * sign
      ToolAdjustMode.YAW -> loc.yaw = ((loc.yaw + yawStep * sign) + 360f) % 360f
    }

    // TextDisplay と Interaction エンティティそのものを同時にテレポート移動
    val display = manager.getDisplay(data.id)
    if (display != null && display.isValid) {
      display.teleport(loc)
    }
    val interaction = manager.getInteraction(data.id)
    if (interaction != null && interaction.isValid) {
      interaction.teleport(loc)
    }

    manager.spawnOrUpdateEntities(data)
    manager.saveAll()

    // サウンド再生
    val sound = if (subtract) Sound.BLOCK_NOTE_BLOCK_BASS else Sound.BLOCK_NOTE_BLOCK_PLING
    player.playSound(player.location, sound, 0.7f, 1.8f)

    // アクションバー表示
    val valStr =
        when (mode) {
          ToolAdjustMode.X -> String.format("%.2f", loc.x)
          ToolAdjustMode.Y -> String.format("%.2f", loc.y)
          ToolAdjustMode.Z -> String.format("%.2f", loc.z)
          ToolAdjustMode.YAW -> String.format("%.1f°", loc.yaw)
        }
    val stepStr = if (mode == ToolAdjustMode.YAW) "${yawStep}°" else mode.stepName
    val arrow = if (subtract) "&c-" else "&a+"

    player.sendActionBar(
        TextFormatter.format("&b[ホロクリスタル] &e${mode.displayName}: &f$valStr &7($arrow$stepStr&7)")
    )
  }

  private fun openMenuIfPermitted(player: Player, data: HologramData) {
    if (data.owner != player.uniqueId && !player.hasPermission("endrollhologram.admin")) {
      player.sendMessage(TextFormatter.format("&c[ホログラム] &7他のプレイヤーが作成したホログラムは編集できません。"))
      player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
      return
    }

    player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f)
    MainMenuGui.open(player, data)
  }
}
