package com.ivandabul.nivelbolhapro

import android.app.Application
import android.content.Context
import android.hardware.*
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
    val pitchDegAdj: Float = 0f,
    val rollDegAdj: Float = 0f,
    val toleranceDeg: Float = 1.0f,
    val maxAngle: Float = 45f,
    val isLeveled: Boolean = false,
    val wasLeveled: Boolean = false,
    val darkTheme: Boolean = false,
    val soundEnabled: Boolean = false,
    val rollPercent: Float = 0f,
    val pitchPercent: Float = 0f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val swapXY: Boolean = false,
)

class SensorsViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {

    private val context = app.applicationContext
    private val sm: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magSensor: Sensor? = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var offsetPitch = 0f
    private var offsetRoll = 0f

    private var fPitch = 0f
    private var fRoll = 0f
    private val alpha = 0.1f

    private var lastLeveled = false

    private val R = FloatArray(9)
    private val I = FloatArray(9)
    private val orientation = FloatArray(3)
    private val accValues = FloatArray(3)
    private val magValues = FloatArray(3)

    private val store = CalibrationStore(context)

    init {
        val s = store.load()
        offsetPitch = s.offsetPitch
        offsetRoll = s.offsetRoll
        _uiState.value = _uiState.value.copy(
            toleranceDeg = s.tolerance,
            darkTheme = s.darkTheme,
            soundEnabled = s.sound,
            invertX = s.invertX,
            invertY = s.invertY,
            swapXY = s.swapXY
        )
        register()
    }

    fun register() {
        if (rotationSensor != null) {
            sm.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magSensor?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    fun unregister() { sm.unregisterListener(this) }

    override fun onCleared() { super.onCleared(); unregister() }

    private fun currentRotation(): Int {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val disp: Display? = dm.getDisplay(Display.DEFAULT_DISPLAY)
        return disp?.rotation ?: Surface.ROTATION_0
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotMat = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotMat, event.values)
                val outR = FloatArray(9)
                when (currentRotation()) {
                    Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR)
                    Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X, outR)
                    Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Z, outR)
                    Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rotMat, SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X, outR)
                }
                val orien = FloatArray(3)
                SensorManager.getOrientation(outR, orien)
                val pitch = Math.toDegrees(orien[1].toDouble()).toFloat()
                val roll  = Math.toDegrees(orien[2].toDouble()).toFloat()
                updateAngles(pitch, roll)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accValues[0] = event.values[0]; accValues[1] = event.values[1]; accValues[2] = event.values[2]
                updateFromAccMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magValues[0] = event.values[0]; magValues[1] = event.values[1]; magValues[2] = event.values[2]
                updateFromAccMag()
            }
        }
    }

    private fun updateFromAccMag() {
        if (SensorManager.getRotationMatrix(R, I, accValues, magValues)) {
            SensorManager.getOrientation(R, orientation)
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll  = Math.toDegrees(orientation[2].toDouble()).toFloat()
            updateAngles(pitch, roll)
        }
    }

    private fun applyUserAxisAdjust(pitch: Float, roll: Float): Pair<Float, Float> {
        var p = pitch; var r = roll
        if (_uiState.value.swapXY) { val t = p; p = r; r = t }
        if (_uiState.value.invertY) p = -p
        if (_uiState.value.invertX) r = -r
        return p to r
    }

    private fun updateAngles(pitchDeg: Float, rollDeg: Float) {
        val (adjP0, adjR0) = applyUserAxisAdjust(pitchDeg, rollDeg)
        val adjP = fPitch + alpha * (adjP0 - fPitch)
        val adjR = fRoll + alpha * (adjR0 - fRoll)
        fPitch = adjP; fRoll = adjR

        val pitchAdj = adjP - offsetPitch
        val rollAdj  = adjR - offsetRoll

        val maxA = _uiState.value.maxAngle
        val pitchPct = (pitchAdj / maxA * 100f).coerceIn(-100f, 100f)
        val rollPct  = (rollAdj / maxA * 100f).coerceIn(-100f, 100f)

        val tol = _uiState.value.toleranceDeg
        val leveled = kotlin.math.abs(pitchAdj) <= tol && kotlin.math.abs(rollAdj) <= tol

        _uiState.value = _uiState.value.copy(
            pitchDegAdj = pitchAdj,
            rollDegAdj = rollAdj,
            isLeveled = leveled,
            wasLeveled = lastLeveled,
            pitchPercent = pitchPct,
            rollPercent = rollPct
        )
        lastLeveled = leveled
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun calibrateX() { offsetRoll = fRoll; persist() }
    fun calibrateY() { offsetPitch = fPitch; persist() }
    fun calibratePlane() { offsetPitch = fPitch; offsetRoll = fRoll; persist() }
    fun calibrateAll() = calibratePlane()
    fun zeroOffsets() { offsetPitch = 0f; offsetRoll = 0f; persist() }

    fun setTolerance(v: Float) { _uiState.value = _uiState.value.copy(toleranceDeg = v); persist() }
    fun setDarkTheme(on: Boolean) { _uiState.value = _uiState.value.copy(darkTheme = on); persist() }
    fun setSound(on: Boolean) { _uiState.value = _uiState.value.copy(soundEnabled = on); persist() }
    fun setInvertX(on: Boolean) { _uiState.value = _uiState.value.copy(invertX = on); persist() }
    fun setInvertY(on: Boolean) { _uiState.value = _uiState.value.copy(invertY = on); persist() }
    fun setSwapXY(on: Boolean) { _uiState.value = _uiState.value.copy(swapXY = on); persist() }

    private fun persist() {
        val s = _uiState.value
        store.save(offsetPitch, offsetRoll, s.toleranceDeg, s.darkTheme, s.soundEnabled, s.invertX, s.invertY, s.swapXY)
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.AndroidViewModelFactory(app) {}
    }
}