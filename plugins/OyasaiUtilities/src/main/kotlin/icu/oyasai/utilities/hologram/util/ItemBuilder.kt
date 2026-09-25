@file:Suppress("DEPRECATION")

package icu.oyasai.utilities.hologram.util

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ItemBuilder(private val material: Material, private val amount: Int = 1) {
  private var name: Component? = null
  private val lore: MutableList<Component> = mutableListOf()
  private val flags: MutableSet<ItemFlag> = mutableSetOf()
  private var glow: Boolean = false
  private var customModelData: Int? = null
  private val stringPdc: MutableMap<NamespacedKey, String> = mutableMapOf()

  constructor(item: ItemStack) : this(item.type, item.amount) {
    val meta = item.itemMeta ?: return
    this.name = meta.displayName()
    meta.lore()?.let { this.lore.addAll(it) }
    this.flags.addAll(meta.itemFlags)
    if (meta.hasCustomModelData()) {
      this.customModelData = meta.customModelData
    }
  }

  fun name(name: Component): ItemBuilder {
    this.name = name
    return this
  }

  fun name(name: String): ItemBuilder {
    this.name = TextFormatter.format(name)
    return this
  }

  fun lore(vararg lines: Component): ItemBuilder {
    this.lore.addAll(lines)
    return this
  }

  fun lore(vararg lines: String): ItemBuilder {
    for (line in lines) {
      this.lore.add(TextFormatter.format(line))
    }
    return this
  }

  fun lore(lines: List<String>): ItemBuilder {
    for (line in lines) {
      this.lore.add(TextFormatter.format(line))
    }
    return this
  }

  fun glow(glow: Boolean = true): ItemBuilder {
    this.glow = glow
    return this
  }

  fun flags(vararg flags: ItemFlag): ItemBuilder {
    this.flags.addAll(flags)
    return this
  }

  fun pdc(key: NamespacedKey, value: String): ItemBuilder {
    this.stringPdc[key] = value
    return this
  }

  fun build(): ItemStack {
    val item = ItemStack(material, amount)
    val meta = item.itemMeta ?: return item

    name?.let { meta.displayName(it) }
    if (lore.isNotEmpty()) {
      meta.lore(lore)
    }
    flags.forEach { meta.addItemFlags(it) }

    if (glow) {
      meta.addEnchant(Enchantment.UNBREAKING, 1, true)
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
    }

    customModelData?.let { meta.setCustomModelData(it) }

    stringPdc.forEach { (key, value) ->
      meta.persistentDataContainer.set(key, PersistentDataType.STRING, value)
    }

    item.itemMeta = meta
    return item
  }
}
