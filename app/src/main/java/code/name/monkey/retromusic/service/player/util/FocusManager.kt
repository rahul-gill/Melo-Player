package code.name.monkey.retromusic.service.player.util

import android.content.Context
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import java.lang.IllegalStateException

class FocusManager(
    context: Context,
    onFocusGain: () -> Unit,
    onFocusLoss: () -> Unit,
    onDuck: () -> Unit
) {
    private var restoreOnFocusGain = false
    private val audioManager: AudioManager =
        ContextCompat.getSystemService(context, AudioManager::class.java)
            ?: throw IllegalStateException("AudioManager coudn't be initialized")
    private val audioFocusRequest: AudioFocusRequestCompat =
        AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { event ->
                when (event) {
                    AudioManager.AUDIOFOCUS_GAIN ->
                        onFocusGain()

                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                        onFocusLoss()

                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onDuck()
                }
            }
            .build()

    fun requestFocus() = AudioManagerCompat.requestAudioFocus(
        audioManager,
        audioFocusRequest
    ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    fun abandonFocus() =
        AudioManagerCompat.abandonAudioFocusRequest(
            audioManager,
            audioFocusRequest
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
}
