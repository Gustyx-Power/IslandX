package id.xms.capsuleedge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import id.xms.capsuleedge.data.repository.SettingsRepository
import id.xms.capsuleedge.service.OverlayService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsRepository = SettingsRepository(context)
                    val autoStart = settingsRepository.isAutoStartEnabled.first()
                    
                    if (autoStart) {
                        withContext(Dispatchers.Main) {
                            OverlayService.start(context)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
