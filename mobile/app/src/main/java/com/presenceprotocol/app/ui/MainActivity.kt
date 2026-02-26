package com.presenceprotocol.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.presenceprotocol.app.ui.theme.PresenceTheme

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by lazy { DashboardViewModelClient.default() }
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.e("PP_BLE", "BOOT: PresenceProtocol started")
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            if (granted) {
                dashboardViewModel.ensureDiscoveryStarted()
            } else {
                Toast.makeText(this, "Presence Protocol requires Bluetooth permissions", Toast.LENGTH_LONG).show()
            }
        }
        setContent { PresenceApp(dashboardViewModel) }
        if (hasBlePermissions()) {
            dashboardViewModel.ensureDiscoveryStarted()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dashboardViewModel.stopDiscovery()
    }

    private fun hasBlePermissions(): Boolean =
        requiredPermissions().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
}

@Composable
private fun PresenceApp(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    PresenceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBar(
                    title = "Presence Protocol",
                    subtitle = "Quiet Mining",
                    pill = if (uiState.isMining) "Mining ON" else "Idle",
                    onLongPress = { viewModel.showDeveloperPanel(true) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PresencePulseHero(uiState)
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryToggle(isMining = uiState.isMining) { viewModel.toggleMining() }
                Spacer(modifier = Modifier.height(12.dp))
                VerifiedCard(uiState)
                Spacer(modifier = Modifier.height(12.dp))
                YieldCard(uiState)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Details & Logs",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clickable { viewModel.showDeveloperPanel(true) }
                )
            }
            if (uiState.showDeveloperPanel) {
                DeveloperPanel(uiState = uiState, dismiss = { viewModel.showDeveloperPanel(false) })
            }
        }
    }
}

@Composable
private fun TopBar(title: String, subtitle: String, pill: String, onLongPress: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = pill, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PresencePulseHero(uiState: DashboardUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val transition = rememberInfiniteTransition()
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing))
            )
            Box(contentAlignment = Alignment.Center) {
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawCircle(
                        color = primaryColor.copy(alpha = pulseAlpha),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.peersNearby.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Peers Nearby", fontSize = 14.sp)
                    Text(text = uiState.statusText, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Presence earns when encounters are mutually signed.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
private fun PrimaryToggle(isMining: Boolean, onToggle: () -> Unit) {
    Button(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text = if (isMining) "Stop Mining" else "Start Mining", fontSize = 16.sp)
    }
}

@Composable
private fun VerifiedCard(uiState: DashboardUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Peers Seen (10m)", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = "Rolling", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = uiState.peersSeenLast10Minutes.toString(), fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Seen", fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = uiState.pendingEncounters.toString(), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Pending", fontSize = 12.sp)
                }
            }
            Text(text = "Based on BLE discovery", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun YieldCard(uiState: DashboardUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Mining Yield", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = "Settles on sync", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Today: +${String.format("%.2f", uiState.todayYield)} PRX", fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Total: ${String.format("%.1f", uiState.totalBalance)} PRX", fontSize = 16.sp)
        }
    }
}

@Composable
private fun DeveloperPanel(uiState: DashboardUiState, dismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { dismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "Developer", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.devLog.forEach { entry ->
                    Text(text = entry, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (uiState.devLog.isEmpty()) {
                    Text(text = "No events yet", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
