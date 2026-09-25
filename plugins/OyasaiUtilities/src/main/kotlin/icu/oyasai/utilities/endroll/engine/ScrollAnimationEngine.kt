package icu.oyasai.utilities.endroll.engine

import icu.oyasai.utilities.endroll.manager.HologramManager
import icu.oyasai.utilities.endroll.model.DisplayMode
import icu.oyasai.utilities.endroll.model.HologramData
import icu.oyasai.utilities.endroll.model.PlayMode
import icu.oyasai.utilities.endroll.model.TextOrientation
import icu.oyasai.utilities.endroll.util.TextFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

class ScrollAnimationEngine(
    private val plugin: JavaPlugin,
    private val manager: HologramManager,
) {
  private var task: BukkitTask? = null

  private class ScrollState(
      var currentStepIndex: Int = 0,
      // tickInStep: 0 = スナップ, 1 = 補間開始, 2以降 = 補間中
      var tickInStep: Int = 0,
      var isPlayerNearby: Boolean = true,
      var checkNearbyCounter: Int = 0,
  )

  private val states = ConcurrentHashMap<UUID, ScrollState>()

  fun start() {
    stop()
    task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tick() }, 1L, 1L)
  }

  fun stop() {
    task?.cancel()
    task = null
    states.clear()
  }

  private fun tick() {
    for (data in manager.getAll()) {
      if (data.mode != DisplayMode.END_ROLL) continue
      if (data.orientation == TextOrientation.VERTICAL) continue
      if (data.lines.isEmpty()) continue

      val state = states.computeIfAbsent(data.id) { ScrollState() }

      // 20 ticks ごとに近接判定を更新
      state.checkNearbyCounter++
      if (state.checkNearbyCounter >= 20) {
        state.checkNearbyCounter = 0
        state.isPlayerNearby = checkPlayerNearby(data)
      }

      when (data.playMode) {
        PlayMode.PROXIMITY -> {
          if (!state.isPlayerNearby) continue
        }
        PlayMode.MANUAL -> {
          if (!data.isPlaying) continue
        }
        PlayMode.ALWAYS -> {}
      }

      val display = manager.getDisplay(data.id) ?: continue
      if (!display.isValid) continue

      val speedTicks = data.scrollSpeed.coerceAtLeast(5)
      val windowLines = data.windowLines.coerceIn(1, 20)
      // MinecraftのFont lineHeightは9px、TextDisplayスケール0.025の理論値(0.225)と実測値(0.25)の中間値0.2375f
      val lineHeight = 0.2375f * data.scale
      val scaleVec = Vector3f(data.scale, data.scale, data.scale)

      val paddedLines = mutableListOf<String>()
      repeat(windowLines) { paddedLines.add("") }
      paddedLines.addAll(data.lines)
      repeat(windowLines) { paddedLines.add("") }

      val totalSteps = paddedLines.size - windowLines
      if (totalSteps <= 0) continue

      if (state.currentStepIndex >= totalSteps) {
        state.currentStepIndex = 0
      }

      // ======================================================
      // Tick 0 (各ステップの開始): テキスト更新 + Y=0 スナップ
      // ======================================================
      if (state.tickInStep == 0) {
        val fromIdx = state.currentStepIndex
        val toIdx = (fromIdx + windowLines + 1).coerceAtMost(paddedLines.size)
        val window = paddedLines.subList(fromIdx, toIdx)
        val joined = TextFormatter.joinLinesSafely(window)
        display.text(TextFormatter.format(joined))

        // Y=0 に即座にリセット（補間なし）
        // ※ この更新は Tick 0 の entity data パケットとして送信される
        display.interpolationDelay = 0
        display.interpolationDuration = 0
        display.transformation =
            Transformation(
                Vector3f(0f, 0f, 0f),
                AxisAngle4f(0f, 0f, 0f, 1f),
                scaleVec,
                AxisAngle4f(0f, 0f, 0f, 1f),
            )
      }

      // ======================================================
      // Tick 1 (次の tick): Y=lineHeight への補間を別パケットで開始
      // Tick 0 と必ず異なる tick になるため、スナップと補間が確実に分離される
      // ======================================================
      if (state.tickInStep == 1) {
        // speedTicks - 1 tick かけて上へスクロール
        // (tick1 〜 tick(speedTicks-1) の間で補間が完了する)
        display.interpolationDelay = 0
        display.interpolationDuration = speedTicks - 1
        display.transformation =
            Transformation(
                Vector3f(0f, lineHeight, 0f),
                AxisAngle4f(0f, 0f, 0f, 1f),
                scaleVec,
                AxisAngle4f(0f, 0f, 0f, 1f),
            )
      }

      state.tickInStep++

      // speedTicks 経過でステップを進める
      if (state.tickInStep >= speedTicks) {
        state.tickInStep = 0
        state.currentStepIndex++
        if (state.currentStepIndex >= totalSteps) {
          state.currentStepIndex = 0
        }
      }
    }
  }

  private fun checkPlayerNearby(data: HologramData): Boolean {
    val loc = data.location
    val world = loc.world ?: return false
    val rangeSq = data.proximityRange * data.proximityRange

    for (player in world.players) {
      if (player.location.distanceSquared(loc) <= rangeSq) {
        return true
      }
    }
    return false
  }

  fun resetState(id: UUID) {
    states.remove(id)
  }
}
