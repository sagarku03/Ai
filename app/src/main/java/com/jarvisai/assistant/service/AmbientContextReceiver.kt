package com.jarvisai.assistant.service

import android.app.UiModeManager
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors Bluetooth Handsfree/Car connection state and Car UI Mode changes,
 * exposing `isDrivingHandsFreeActive` so JARVIS can automatically enable hands-free voice responses.
 */
class AmbientContextReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                val connected = state == BluetoothProfile.STATE_CONNECTED
                _isBluetoothHandsFreeConnected.value = connected
                updateDrivingModeState()
                Log.i(TAG, "Bluetooth HandsFree connected=$connected")
            }
            UiModeManager.ACTION_ENTER_CAR_MODE -> {
                _isCarModeActive.value = true
                updateDrivingModeState()
                Log.i(TAG, "Entered Car Mode")
            }
            UiModeManager.ACTION_EXIT_CAR_MODE -> {
                _isCarModeActive.value = false
                updateDrivingModeState()
                Log.i(TAG, "Exited Car Mode")
            }
        }
    }

    private fun updateDrivingModeState() {
        _isDrivingHandsFreeActive.value = _isCarModeActive.value || _isBluetoothHandsFreeConnected.value
    }

    companion object {
        private const val TAG = "AmbientContextReceiver"

        private val _isBluetoothHandsFreeConnected = MutableStateFlow(false)
        val isBluetoothHandsFreeConnected: StateFlow<Boolean> = _isBluetoothHandsFreeConnected.asStateFlow()

        private val _isCarModeActive = MutableStateFlow(false)
        val isCarModeActive: StateFlow<Boolean> = _isCarModeActive.asStateFlow()

        private val _isDrivingHandsFreeActive = MutableStateFlow(false)
        val isDrivingHandsFreeActive: StateFlow<Boolean> = _isDrivingHandsFreeActive.asStateFlow()
    }
}
