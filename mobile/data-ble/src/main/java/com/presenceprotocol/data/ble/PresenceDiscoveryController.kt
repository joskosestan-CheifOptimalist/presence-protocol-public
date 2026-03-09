package com.presenceprotocol.data.ble

import com.presenceprotocol.data.ble.gatt.PresenceGattServer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.presenceprotocol.data.ble.gatt.PresenceGattClient
import com.presenceprotocol.domain.discovery.PeerDiscoveryMetrics
import com.presenceprotocol.domain.discovery.PeerEvent
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PresenceDiscoveryController(
    private val context: Context,
    private val presenceGattServer: PresenceGattServer,
    private val miningLedger: com.presenceprotocol.domain.MiningLedger,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val serviceParcelUuid = ParcelUuid(PRESENCE_SERVICE_UUID)

    private val _metrics = MutableStateFlow(PeerDiscoveryMetrics())
    val metrics: StateFlow<PeerDiscoveryMetrics> = _metrics.asStateFlow()

    private val _peerEvents = MutableSharedFlow<PeerEvent>(extraBufferCapacity = 32)
    val peerEvents: SharedFlow<PeerEvent> = _peerEvents.asSharedFlow()

    private val lastSeenMap = mutableMapOf<String, Long>()
    private val handshakeCoordinator by lazy { PresenceHandshakeCoordinator(bluetoothAdapter, miningLedger) }
    private val gattClient by lazy { PresenceGattClient(context, handshakeCoordinator) }

    private val started = AtomicBoolean(false)
    private var cleanupJob: Job? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "Advertising started: $settingsInEffect")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "Advertising failed: $errorCode")
        }
    }

    fun start() {
        if (!hasBlePermissions()) {
            Log.w(TAG, "Missing BLE permissions; discovery not started")
            return
        }
        if (!started.compareAndSet(false, true)) return
        if (bluetoothAdapter?.isEnabled != true) {
            Log.w(TAG, "Bluetooth adapter disabled")
            started.set(false)
            return
        }
        Log.e(TAG, "PP_DISCOVERY: start() -> PP_DISCOVERY START ")
        startAdvertising()
        startScanning()
        cleanupJob = scope.launch { pruneLoop() }
        Log.d(TAG, "Presence BLE discovery started")
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        stopScanning()
        stopAdvertising()
        cleanupJob?.cancel()
        cleanupJob = null
        Log.d(TAG, "Presence BLE discovery stopped")
    }

    private fun hasBlePermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        val scanner: BluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder().setServiceUuid(serviceParcelUuid).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
        Log.e(TAG, "PP_DISCOVERY: startScanning() -> PP_DISCOVERY START_SCAN filters=${filter}")
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        val advertiser: BluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setTimeout(0)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(serviceParcelUuid)
            .setIncludeDeviceName(false)
            .build()
        advertiser.startAdvertising(settings, data, advertiseCallback)
        Log.e(TAG, "PP_DISCOVERY: startAdvertising() -> PP_DISCOVERY START_ADVERTISE")
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }

    private fun handleScanResult(result: ScanResult) {
        if (!hasBlePermissions()) return
        val device = result.device ?: return
        val peerId = device.address ?: return
        val scanRecord = result.scanRecord
        val serviceUuids = scanRecord?.serviceUuids?.joinToString(",") { it.uuid.toString() } ?: "none"
        val deviceName = try { device.name } catch (_: SecurityException) { null }

        val nowElapsed = SystemClock.elapsedRealtime()
        val previousSeen = lastSeenMap[peerId]
        if (previousSeen == null || nowElapsed - previousSeen > PEER_LOG_WINDOW_MS) {
            Log.e(
                TAG,
                "PP_DISCOVERY TARGET_MATCH addr=" + device.address + " name=" + deviceName + " rssi=" + result.rssi + " uuids=" + serviceUuids
            )
        }
        lastSeenMap[peerId] = nowElapsed
        handshakeCoordinator.recordSeen(peerId)

        if (previousSeen == null || nowElapsed - previousSeen > PEER_LOG_WINDOW_MS) {
            Log.e(TAG, "PP_DISCOVERY PEER_SEEN addr=" + device.address + " rssi=" + result.rssi)
            _peerEvents.tryEmit(PeerEvent(peerId, Instant.now()))
        }

        if (handshakeCoordinator.shouldInitiate(device)) {
            handshakeCoordinator.markConnectStart(peerId)
            gattClient.onPeerSeen(device)
        }

        updateMetrics(nowElapsed)
    }

    private suspend fun pruneLoop() {
        while (started.get()) {
            val now = SystemClock.elapsedRealtime()
            pruneOldEntries(now)
            updateMetrics(now)
            kotlinx.coroutines.delay(PRUNE_INTERVAL_MS)
        }
    }

    private fun pruneOldEntries(now: Long) {
        val iterator = lastSeenMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > TEN_MINUTES_MS) {
                iterator.remove()
            }
        }
    }

    private fun updateMetrics(now: Long) {
        val nearbyCount = lastSeenMap.count { now - it.value <= NEARBY_WINDOW_MS }
        val seen10m = lastSeenMap.count { now - it.value <= TEN_MINUTES_MS }
        _metrics.value = PeerDiscoveryMetrics(nearbyCount, seen10m)
    }

    companion object {
        private const val TAG = "PresenceDiscovery"
        private const val NEARBY_WINDOW_MS = 10_000L
        private const val PEER_LOG_WINDOW_MS = 15_000L
        private const val TEN_MINUTES_MS = 10 * 60 * 1000L
        private const val PRUNE_INTERVAL_MS = 5_000L

        val PRESENCE_SERVICE_UUID: UUID = UUID.fromString("7d3a2d6b-9b7a-4f2a-9e5e-0c9d6f1b1c01")
    }
}
