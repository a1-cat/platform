package icu.oyasai.utilities.hologram.storage

import icu.oyasai.utilities.hologram.model.HologramData
import java.io.File
import java.util.UUID
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

class YamlStorage(private val plugin: JavaPlugin) {
  private val dataFile = File(plugin.dataFolder, "holograms.yml")
  private var config = YamlConfiguration()

  init {
    if (!plugin.dataFolder.exists()) {
      plugin.dataFolder.mkdirs()
    }
    // 旧プラグイン(EndRollHologram)からのマイグレーション
    if (!dataFile.exists()) {
      val oldFile = File(plugin.dataFolder.parentFile, "EndRollHologram/data.yml")
      if (oldFile.exists()) {
        try {
          oldFile.copyTo(dataFile)
          plugin.logger.info("EndRollHologram/data.yml からホログラムデータを自動移行しました。")
        } catch (e: Exception) {
          plugin.logger.warning("旧ホログラムデータの移行に失敗しました: ${e.message}")
          dataFile.createNewFile()
        }
      } else {
        dataFile.createNewFile()
      }
    }
  }

  fun loadAll(): Map<UUID, HologramData> {
    config = YamlConfiguration.loadConfiguration(dataFile)
    val map = mutableMapOf<UUID, HologramData>()

    val section = config.getConfigurationSection("holograms") ?: return map
    for (key in section.getKeys(false)) {
      val subSection = section.getConfigurationSection(key) ?: continue
      val values = subSection.getValues(false)
      val data = HologramData.deserialize(values)
      if (data != null) {
        map[data.id] = data
      }
    }
    return map
  }

  fun saveAll(holograms: Collection<HologramData>) {
    config = YamlConfiguration()
    for (holo in holograms) {
      val serialized = holo.serialize()
      val key = holo.id.toString()
      config.createSection("holograms.$key", serialized)
    }
    try {
      config.save(dataFile)
    } catch (e: Exception) {
      plugin.logger.severe("data.ymlの保存中にエラーが発生しました: ${e.message}")
    }
  }
}
