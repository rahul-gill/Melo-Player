package meloplayer.app.service

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import meloplayer.app.model.Song
import meloplayer.app.service.playback.Playback
import meloplayer.app.util.PreferenceUtil

interface PlaybackManager {
    fun setCallbacks(callbacks: Playback.PlaybackCallbacks)
    val audioSessionId: Int?
    val songDurationMillis: Int?
    val songProgressMillis: Int?
    val isPlaying: Boolean
    fun setPlaybackSpeedPitch(speed: Float, pitch:Float)
    fun maybeSwitchToCrossFade(crossFadeDuration: Int): Boolean
    fun setCrossFadeDuration(crossFadeDuration: Int)
    fun setNextDataSource(uri: Uri?)
    fun pause(force: Boolean, onFinish: () -> Unit)
    fun seek(millis: Int, force: Boolean): Int
    fun setDataSource(song: Song, force: Boolean, completion: (Boolean) -> Unit )
    fun play(onNotInitialized: () -> Unit)
    fun release()
}

class PlaybackManagerImpl(val context: Context) : PlaybackManager{

    var playback: Playback? = null
    private var playbackLocation = PlaybackLocation.LOCAL

    val isLocalPlayback get() = playbackLocation == PlaybackLocation.LOCAL

    override val audioSessionId: Int
        get() = if (playback != null) {
            playback!!.audioSessionId
        } else 0

    override val songDurationMillis: Int
        get() = if (playback != null) {
            playback!!.duration()
        } else -1

    override val songProgressMillis: Int
        get() = if (playback != null) {
            playback!!.position()
        } else -1

    override val isPlaying: Boolean
        get() = playback != null && playback!!.isPlaying

    init {
        playback = createLocalPlayback()
    }

    override fun setCallbacks(callbacks: Playback.PlaybackCallbacks) {
        playback?.callbacks = callbacks
    }

    override fun play(onNotInitialized: () -> Unit) {
        if (playback != null && !playback!!.isPlaying) {
            if (!playback!!.isInitialized) {
                onNotInitialized()
            } else {
                openAudioEffectSession()
                if (playbackLocation == PlaybackLocation.LOCAL) {
                    if (playback is CrossFadePlayer) {
                        if (!(playback as CrossFadePlayer).isCrossFading) {
                            AudioFader.startFadeAnimator(playback!!, true)
                        }
                    } else {
                        AudioFader.startFadeAnimator(playback!!, true)
                    }
                }
                playback?.start()
            }
        }
    }


    override fun pause(force: Boolean, onPause: () -> Unit) {
        if (playback != null && playback!!.isPlaying) {
            if (force) {
                playback?.pause()
                closeAudioEffectSession()
                onPause()
            } else {
                AudioFader.startFadeAnimator(playback!!, false) {
                    //Code to run when Animator Ends
                    playback?.pause()
                    closeAudioEffectSession()
                    onPause()
                }
            }
        }
    }

    override fun seek(millis: Int, force: Boolean): Int = playback!!.seek(millis, force)



    override fun setDataSource(
        song: Song,
        force: Boolean,
        completion: (success: Boolean) -> Unit,
    ) {
        playback?.setDataSource(song, force, completion)
    }

    override fun setNextDataSource(trackUri: Uri?) {
        playback?.setNextDataSource(trackUri?.toString())
    }

    override fun setCrossFadeDuration(duration: Int) {
        playback?.setCrossFadeDuration(duration)
    }

    /**
     * @param crossFadeDuration CrossFade duration
     * @return Whether switched playback
     */
    override fun maybeSwitchToCrossFade(crossFadeDuration: Int): Boolean {
        /* Switch to MultiPlayer if CrossFade duration is 0 and
                Playback is not an instance of MultiPlayer */
        if (playback !is MultiPlayer && crossFadeDuration == 0) {
            if (playback != null) {
                playback?.release()
            }
            playback = null
            playback = MultiPlayer(context)
            return true
        } else if (playback !is CrossFadePlayer && crossFadeDuration > 0) {
            if (playback != null) {
                playback?.release()
            }
            playback = null
            playback = CrossFadePlayer(context)
            return true
        }
        return false
    }

    override fun release() {
        playback?.release()
        playback = null
        closeAudioEffectSession()
    }

    private fun openAudioEffectSession() {
        val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        context.sendBroadcast(intent)
    }

    private fun closeAudioEffectSession() {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        if (playback != null) {
            audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                playback!!.audioSessionId)
        }
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(audioEffectsIntent)
    }

    fun switchToLocalPlayback(onChange: (wasPlaying: Boolean, progress: Int) -> Unit) {
        playbackLocation = PlaybackLocation.LOCAL
        switchToPlayback(createLocalPlayback(), onChange)
    }

    fun switchToRemotePlayback(
        castPlayer: CastPlayer,
        onChange: (wasPlaying: Boolean, progress: Int) -> Unit,
    ) {
        playbackLocation = PlaybackLocation.REMOTE
        switchToPlayback(castPlayer, onChange)
    }

    private fun switchToPlayback(
        playback: Playback,
        onChange: (wasPlaying: Boolean, progress: Int) -> Unit,
    ) {
        val oldPlayback = this.playback
        val wasPlaying: Boolean = oldPlayback?.isPlaying == true
        val progress: Int = oldPlayback?.position() ?: 0
        this.playback = playback
        oldPlayback?.stop()
        onChange(wasPlaying, progress)
    }

    private fun createLocalPlayback(): Playback {
        // Set MultiPlayer when crossfade duration is 0 i.e. off
        return if (PreferenceUtil.crossFadeDuration == 0) {
            MultiPlayer(context)
        } else {
            CrossFadePlayer(context)
        }
    }

    override fun setPlaybackSpeedPitch(playbackSpeed: Float, playbackPitch: Float) {
        playback?.setPlaybackSpeedPitch(playbackSpeed, playbackPitch)
    }
}

enum class PlaybackLocation {
    LOCAL,
    REMOTE
}