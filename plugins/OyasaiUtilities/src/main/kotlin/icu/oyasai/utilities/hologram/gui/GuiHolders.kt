package icu.oyasai.utilities.hologram.gui

import java.util.UUID
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class MainMenuHolder(val hologramId: UUID) : InventoryHolder {
  private lateinit var inventory: Inventory

  fun setInventory(inv: Inventory) {
    this.inventory = inv
  }

  override fun getInventory(): Inventory = inventory
}

class TextEditorHolder(val hologramId: UUID, var page: Int = 0) : InventoryHolder {
  private lateinit var inventory: Inventory

  fun setInventory(inv: Inventory) {
    this.inventory = inv
  }

  override fun getInventory(): Inventory = inventory
}

class DeleteConfirmHolder(val hologramId: UUID) : InventoryHolder {
  private lateinit var inventory: Inventory

  fun setInventory(inv: Inventory) {
    this.inventory = inv
  }

  override fun getInventory(): Inventory = inventory
}

class ColorPaletteHolder(val hologramId: UUID, val lineIndex: Int) : InventoryHolder {
  private lateinit var inventory: Inventory

  fun setInventory(inv: Inventory) {
    this.inventory = inv
  }

  override fun getInventory(): Inventory = inventory
}
