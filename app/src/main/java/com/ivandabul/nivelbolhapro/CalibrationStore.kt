package com.ivandabul.nivelbolhapro

import android.content.Context
import android.content.SharedPreferences

class CalibrationStore(ctx: Context) {
    private val prefs: SharedPreferences = ctx.getSharedPreferences("calibration", Context.MODE_PRIVATE)

    fun save(
        offsetPitch: Float,
        offsetRoll: Float,
        tolerance: Float,
        darkTheme: Boolean,
        sound: Boolean,
        invertX: Boolean,
        invertY: Boolean,
        swapXY: Boolean
    ) {
        prefs.edit()
            .putFloat("offsetPitch", offsetPitch)
            .putFloat("offsetRoll", offsetRoll)
            .putFloat("tolerance", tolerance)
            .putBoolean("darkTheme", darkTheme)
            .putBoolean("sound", sound)
            .putBoolean("invertX", invertX)
            .putBoolean("invertY", invertY)
            .putBoolean("swapXY", swapXY)
            .apply()
    }

    fun load(): StoreData {
        val p = prefs.getFloat("offsetPitch", 0f)
        val r = prefs.getFloat("offsetRoll", 0f)
        val t = prefs.getFloat("tolerance", 1.0f)
        val d = prefs.getBoolean("darkTheme", false)
        val s = prefs.getBoolean("sound", false)
        val ix = prefs.getBoolean("invertX", false)
        val iy = prefs.getBoolean("invertY", false)
        val sw = prefs.getBoolean("swapXY", false)
        return StoreData(p, r, t, d, s, ix, iy, sw)
    }
}

data class StoreData(
    val offsetPitch: Float,
    val offsetRoll: Float,
    val tolerance: Float,
    val darkTheme: Boolean,
    val sound: Boolean,
    val invertX: Boolean,
    val invertY: Boolean,
    val swapXY: Boolean
)