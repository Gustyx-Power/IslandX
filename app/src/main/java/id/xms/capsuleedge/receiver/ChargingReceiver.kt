package id.xms.capsuleedge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import id.xms.capsuleedge.data.repository.IslandStateRepository
import id.xms.capsuleedge.domain.model.IslandEvent
import kotlinx.coroutines.*

class ChargingReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                handleChargingConnected(context)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                handleChargingDisconnected()
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                handleBatteryChanged(intent)
            }
        }
    }
    
    private fun handleChargingConnected(context: Context) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        // Check if fast charging by looking at current charging speed
        val isFastCharging = try {
            val chargingCurrent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            // Fast charging typically > 1500mA (1.5A)
            kotlin.math.abs(chargingCurrent) > 1500000
        } catch (e: Exception) {
            false
        }
        
        val chargingEvent = IslandEvent.Charging(
            batteryLevel = batteryLevel,
            isCharging = true,
            isFastCharging = isFastCharging
        )
        
        IslandStateRepository.pushEvent(chargingEvent)
        
        // Auto-dismiss charging notification after 3 seconds
        scope.launch {
            delay(3000)
            val currentEvent = IslandStateRepository.uiState.value.currentEvent
            if (currentEvent is IslandEvent.Charging) {
                IslandStateRepository.dismissCurrentEvent()
            }
        }
    }
    
    private fun handleChargingDisconnected() {
        // Dismiss any charging event
        val currentEvent = IslandStateRepository.uiState.value.currentEvent
        if (currentEvent is IslandEvent.Charging) {
            IslandStateRepository.dismissCurrentEvent()
        }
    }
    
    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryLevel = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            -1
        }
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        // Update current charging event if one is displayed
        val currentEvent = IslandStateRepository.uiState.value.currentEvent
        if (currentEvent is IslandEvent.Charging && isCharging) {
            val chargingType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val isFastCharging = chargingType == BatteryManager.BATTERY_PLUGGED_AC
            
            val updatedEvent = currentEvent.copy(
                batteryLevel = batteryLevel,
                isFastCharging = isFastCharging
            )
            IslandStateRepository.displayEvent(updatedEvent)
        }
    }
}
