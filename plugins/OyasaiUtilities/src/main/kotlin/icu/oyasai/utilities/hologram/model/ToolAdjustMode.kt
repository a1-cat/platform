package icu.oyasai.utilities.hologram.model

enum class ToolAdjustMode(val displayName: String, val stepName: String) {
  X("X座標", "0.1"),
  Y("Y座標", "0.1"),
  Z("Z座標", "0.1"),
  YAW("向き (Yaw)", "15°");

  fun next(): ToolAdjustMode =
      when (this) {
        X -> Y
        Y -> Z
        Z -> YAW
        YAW -> X
      }
}
