package id.xms.capsuleedge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import id.xms.capsuleedge.data.repository.IslandStateRepository
import id.xms.capsuleedge.domain.model.IslandEvent
import id.xms.capsuleedge.domain.model.SoundMode
import kotlinx.coroutines.*

class RingerModeReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return
        
        if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val ringerMode = audioManager.ringerMode
            
            val soundMode = when (ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> SoundMode.SILENT
                AudioManager.RINGER_MODE_VIBRATE -> SoundMode.VIBRATE
                AudioManager.RINGER_MODE_NORMAL -> SoundMode.NORMAL
                else -> SoundMode.NORMAL
            }
            
            val ringerEvent = IslandEvent.RingerMode(mode = soundMode)
            IslandStateRepository.pushEvent(ringerEvent)
            
            // Auto-dismiss after 2 seconds
            scope.launch {
                delay(2000)
                val currentEvent = IslandStateRepository.uiState.value.currentEvent
                if (currentEvent is IslandEvent.RingerMode) {
                    IslandStateRepository.dismissCurrentEvent()
                }
            }
        }
    }
}
