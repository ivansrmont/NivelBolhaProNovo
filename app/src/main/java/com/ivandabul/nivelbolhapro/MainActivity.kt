package com.ivandabul.nivelbolhapro

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.drawToBitmap
import com.ivandabul.nivelbolhapro.ui.BubbleView2D
import com.ivandabul.nivelbolhapro.ui.BubbleViewX
import com.ivandabul.nivelbolhapro.ui.BubbleViewY
import com.ivandabul.nivelbolhapro.ui.theme.NivelBolhaTheme

class MainActivity : ComponentActivity() {

    private val vm: SensorsViewModel by viewModels {
        SensorsViewModel.factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            NivelBolhaTheme {
                AppScreen(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: SensorsViewModel) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(uiState.isLeveled) {
        if (uiState.isLeveled && !uiState.wasLeveled) {
            vibrate(context, 100)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("NivelBolha Pro") }) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // X (top)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Nível X (roll γ)", fontWeight = FontWeight.Bold)
                        BubbleViewX(angleDeg = uiState.rollDegAdj, maxAngle = uiState.maxAngle, width = 280.dp, height = 48.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("X: %.1f° (%.0f%%)".format(uiState.rollDegAdj, uiState.rollPercent))
                    }
                }

                // Middle: Y + circular
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Card(Modifier.weight(0.35f)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nível Y (pitch β)", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            BubbleViewY(angleDeg = uiState.pitchDegAdj, maxAngle = uiState.maxAngle, width = 48.dp, height = 220.dp)
                            Spacer(Modifier.height(8.dp))
                            Text("Y: %.1f° (%.0f%%)".format(uiState.pitchDegAdj, uiState.pitchPercent))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Card(Modifier.weight(0.65f)) {
                        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nível de Plano (2D)", fontWeight = FontWeight.Bold)
                            BubbleView2D(angleXDeg = uiState.rollDegAdj, angleYDeg = uiState.pitchDegAdj, maxAngle = uiState.maxAngle, size = 260.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(if (uiState.isLeveled) "NIVELADO" else "—", color = if (uiState.isLeveled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                // Controls
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Controles", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.calibrateX() }) { Text("Calibrar X") }
                            Button(onClick = { vm.calibrateY() }) { Text("Calibrar Y") }
                            Button(onClick = { vm.calibratePlane() }) { Text("Calibrar Plano") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.calibrateAll() }) { Text("Calibrar Todos") }
                            OutlinedButton(onClick = { vm.zeroOffsets() }) { Text("Zerar") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tolerância: ±${'$'}{uiState.toleranceDeg}°")
                            Slider(value = uiState.toleranceDeg, onValueChange = { vm.setTolerance(it) }, valueRange = 0.5f..2.0f, steps = 2)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Checkbox(checked = uiState.invertX, onCheckedChange = { vm.setInvertX(it) }); Text("Inverter X")
                            Spacer(Modifier.width(16.dp))
                            Checkbox(checked = uiState.invertY, onCheckedChange = { vm.setInvertY(it) }); Text("Inverter Y")
                            Spacer(Modifier.width(16.dp))
                            Checkbox(checked = uiState.swapXY, onCheckedChange = { vm.setSwapXY(it) }); Text("Trocar X↔Y")
                            Spacer(Modifier.width(16.dp))
                            val bmp = view.drawToBitmap()
                            Button(onClick = { ShareUtils.shareBitmap(context, bmp, "nivelbolha_${System.currentTimeMillis()}.png") }) { Text("Screenshot") }
                        }
                    }
                }
            }
        }
    }
}

private fun vibrate(context: Context, durationMs: Long) {
    val vibMgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    val vibrator: Vibrator? = vibMgr?.defaultVibrator ?: context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
}