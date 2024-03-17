package code.name.monkey.retromusic.service.player.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat

class NoisyDeviceSwitchWatcher(private val context: Context, onGettingNoisy: () -> Unit) {
    private var isRegistered = false


    private val becomingNoisyReceiverIntentFilter =
        IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null
                && intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY
            ){
                onGettingNoisy()
            }
        }
    }

    fun register() {

        if (isRegistered) {
            return
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            becomingNoisyReceiverIntentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        isRegistered = true
    }

    fun unregister() {
        if (isRegistered) {
            context.unregisterReceiver(receiver)
            isRegistered = false
        }
    }
}