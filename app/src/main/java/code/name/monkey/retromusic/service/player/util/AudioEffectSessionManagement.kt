package code.name.monkey.retromusic.service.player.util

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect

class AudioEffectSessionManagement(
    private val context: Context
) {

    fun openAudioEffectSession(audioSessionId: Int) {
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        context.sendBroadcast(intent)
    }

    fun closeAudioEffectSession(audioSessionId: Int?) {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        if (audioSessionId != null) {
            audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        }
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(audioEffectsIntent)
    }
}