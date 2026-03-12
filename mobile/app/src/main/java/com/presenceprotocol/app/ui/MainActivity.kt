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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
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
import com.presenceprotocol.app.ui.theme.Olive
import com.presenceprotocol.app.ui.theme.OlivePale
import com.presenceprotocol.app.ui.theme.GoldBright
import com.presenceprotocol.app.ui.theme.GoldLight
import com.presenceprotocol.app.ui.theme.Cream
import com.presenceprotocol.app.ui.theme.Dark
import com.presenceprotocol.app.ui.theme.Mid
import com.presenceprotocol.app.ui.theme.Gray
import com.presenceprotocol.app.ui.theme.Gold
import com.presenceprotocol.app.ui.theme.GoldPale
import com.presenceprotocol.app.ui.theme.LayerMobile
import com.presenceprotocol.app.ui.theme.LayerEncounter
import com.presenceprotocol.app.ui.theme.LayerRelay
import com.presenceprotocol.app.ui.theme.LayerMidnight
import com.presenceprotocol.app.ui.theme.LayerCardano

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBar(
                    title = "Presence Protocol",
                    subtitle = "Quiet Mining",
                    pill = if (uiState.isMining) "Mining ON" else "Idle",
                    onLongPress = { viewModel.showDeveloperPanel(true) }
                )
                PresencePulseHero(uiState)
                PrimaryToggle(isMining = uiState.isMining) { viewModel.toggleMining() }
                VerifiedCard(uiState)
                YieldCard(uiState)
                MiningCountersCard(uiState)
                SettlementLayerCard(uiState)
                Text(
                    text = "Details & Logs",
                    color = GoldLight,
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
            Text(text = subtitle, fontSize = 12.sp, color = GoldLight)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(OlivePale.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
        }
    }
}

@Composable
private fun PresencePulseHero(uiState: DashboardUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp),
        colors = CardDefaults.cardColors(containerColor = OlivePale),
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
                val primaryColor = GoldBright
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawCircle(
                        color = primaryColor.copy(alpha = pulseAlpha),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Peers Nearby", fontSize = 14.sp)
                    Text(text = uiState.statusText, fontSize = 12.sp, color = Gray)
                }
            }
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
        colors = ButtonDefaults.buttonColors(containerColor = GoldBright, contentColor = Dark)
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
        colors = CardDefaults.cardColors(containerColor = OlivePale),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Peers Seen (10m)", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = "Rolling", fontSize = 12.sp, color = Gray)
            }
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Seen", fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Pending", fontSize = 12.sp)
                }
            }
            Text(text = "Based on BLE discovery", fontSize = 11.sp, color = Gray)
        }
    }
}

@Composable
private fun YieldCard(uiState: DashboardUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        colors = CardDefaults.cardColors(containerColor = OlivePale),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Mining Yield", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = "Settles on sync", fontSize = 12.sp, color = Gray)
            }
            Text(text = "Total: ${String.format("%.1f", uiState.totalBalance)} POP", fontSize = 16.sp)
        }
    }
}

@Composable
private fun MiningCountersCard(uiState: DashboardUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        colors = CardDefaults.cardColors(containerColor = OlivePale),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Mining Counters", fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(text = "Protocol Epoch ${uiState.epoch}", fontSize = 12.sp, color = Gray)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "This Epoch", fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Total", fontSize = 12.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Pending", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettlementLayerCard(uiState: DashboardUiState) {
    var showWalletPreview by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp),
        colors = CardDefaults.cardColors(containerColor = GoldPale),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Wallet Preview", fontSize = 12.sp, color = Gold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayerChip("Mobile", LayerMobile)
                LayerChip("Encounter", LayerEncounter)
                LayerChip("Relay", LayerRelay)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LayerChip("Midnight", LayerMidnight)
                LayerChip("Cardano", LayerCardano)
            }
            OutlinedButton(
                onClick = { showWalletPreview = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Olive),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Olive
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Wallet Preview",
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "Preview future wallet connection and settlement",
                        fontSize = 11.sp,
                        color = Mid
                    )
                }
            }


            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Claimable", fontSize = 12.sp, color = Gray)
                Text(
                    text = String.format("%.2f POP", uiState.totalBalance),
                    fontSize = 18.sp,
                    color = Gold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showWalletPreview = true },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, Olive),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Olive
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Wallet Preview",
                        fontSize = 13.sp,
                    )
                }

                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Olive,
                        contentColor = GoldPale,
                        disabledContainerColor = Olive.copy(alpha = 0.45f),
                        disabledContentColor = GoldPale.copy(alpha = 0.75f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Claim POP",
                        fontSize = 13.sp,
                    )
                }
            }

            Text(text = "Tap Wallet Preview for future settlement view.", fontSize = 12.sp, color = Mid)
        }
    }

    if (showWalletPreview) {
        AlertDialog(
            onDismissRequest = { showWalletPreview = false },
            title = {
                Text(
                    text = "Wallet Preview",
                    color = Olive,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This panel previews a future settlement experience for Presence Protocol.",
                        fontSize = 14.sp,
                        color = Dark
                    )
                    Text(
                        text = "Wallet connection, signing, and claim flows are not active yet.",
                        fontSize = 13.sp,
                        color = Mid
                    )
                    Text(
                        text = "Mining remains separate from wallet and settlement logic.",
                        fontSize = 13.sp,
                        color = Mid
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWalletPreview = false }) {
                    Text("Close", color = Olive, fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = GoldPale,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun LayerChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
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
                .fillMaxHeight(0.72f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {


                DebugRow("Mining", if (uiState.isMining) "ON" else "OFF")
                DebugRow("Debug State", uiState.debugState)
                DebugRow("Status", uiState.statusText)
                DebugRow("Network", uiState.networkHealth)
                DebugRow("Last Peer Seen", uiState.lastPeerSeenId)
                DebugRow("Peers Nearby", uiState.peersNearby.toString())
                DebugRow("Peers Seen (10m)", uiState.peersSeenLast10Minutes.toString())
                DebugRow("Pending", uiState.pendingEncounters.toString())
                DebugRow("Verified Today", uiState.verifiedToday.toString())
                DebugRow("Heartbeat", uiState.heartbeatTick.toString())
                DebugRow("Epoch", uiState.epoch.toString())


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    uiState.devLog.forEach { entry ->
                        Text(text = entry, fontSize = 12.sp)
                    }
                    if (uiState.devLog.isEmpty()) {
                        Text(text = "No events yet", fontSize = 12.sp, color = Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Gray)
    }
}
