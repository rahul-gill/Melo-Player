package code.name.monkey.retromusic.service.player

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import code.name.monkey.retromusic.service.player.util.Fader
import code.name.monkey.retromusic.util.PreferenceUtil
import timber.log.Timber
import java.util.Timer

data class PlaybackPosition(
    val played: Long,
    val total: Long,
) {
    val ratio: Float
        get() = (played.toFloat() / total).takeIf { it.isFinite() } ?: 0f

    companion object {
        val zero = PlaybackPosition(0L, 0L)
    }
}

typealias RadioPlayerOnPreparedListener = (SingleMediaPlayerWrapper) -> Unit
typealias RadioPlayerOnPlaybackPositionListener = (PlaybackPosition) -> Unit
typealias RadioPlayerOnFinishListener = () -> Unit
typealias RadioPlayerOnErrorListener = (Int, Int) -> Unit

class SingleMediaPlayerWrapper constructor(
    private val context: Context,
    uri: Uri,
    var onFinish: RadioPlayerOnFinishListener? = null,
     var onError: RadioPlayerOnErrorListener? = null,
     var onPrepared: RadioPlayerOnPreparedListener? = null
) {
    var usable = false
    var hasPlayedOnce = false

    private val unsafeMediaPlayer: MediaPlayer
    private val mediaPlayer: MediaPlayer?
        get() = if (usable) unsafeMediaPlayer else null

    private var onPlaybackPosition: RadioPlayerOnPlaybackPositionListener? = null

    private var playbackPositionUpdater: Timer? = null

    val playbackPosition: PlaybackPosition?
        get() = mediaPlayer?.let {
            try {
                PlaybackPosition(
                    played = it.currentPosition.toLong(),
                    total = it.duration.toLong(),
                )
            } catch (_: IllegalStateException) {
                null
            }
        }

    var volume: Float = MAX_VOLUME
    var speed: Float = DEFAULT_SPEED
    var pitch: Float = DEFAULT_PITCH
    val fadePlayback: Boolean
        get() = PreferenceUtil.isCrossfadeEnabled
    var audioSessionId: Int?
        get() = mediaPlayer?.audioSessionId
        set(value) {
            if (value != null) {
                mediaPlayer?.audioSessionId = value
            }
        }

    var fadePlaybackDuration: Int = (PreferenceUtil.crossFadeDuration * 1000)
    private var fader: Fader? = null
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying ?: false

    init {
        unsafeMediaPlayer = MediaPlayer().also { ump ->

            ump.setOnPreparedListener {
                usable = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ump.playbackParams.setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
                }
                createDurationTimer()
                onPrepared?.invoke(this)
            }
            ump.setOnCompletionListener {
                usable = false
                onFinish?.invoke()
            }
            ump.setOnErrorListener { _, what, extra ->
                usable = false
                onError?.invoke(what, extra)
                true
            }
            ump.setDataSource(context, uri)
            ump.prepareAsync()
        }
    }


    fun cleanUpBeforeDestroy() {
        Timber.d("cleanUpBeforeDestroy start")
        usable = false
        destroyDurationTimer()
        unsafeMediaPlayer.stop()
        unsafeMediaPlayer.reset()
        unsafeMediaPlayer.release()
        Timber.d("cleanUpBeforeDestroy end")
    }

    fun start(){
        Timber.d("'start' start")
        mediaPlayer?.let {
            it.start()
            if (!hasPlayedOnce) {
                hasPlayedOnce = true
                setSpeed(speed)
                setPitch(pitch)
            }
        }
        Timber.d("'start' end")
    }

    fun pause(){
        Timber.d("'pause' start")
        mediaPlayer?.pause()
        Timber.d("'pause' end")
    }
    fun seek(to: Int){
        Timber.d("'seek' start")
        mediaPlayer?.seekTo(to)
        Timber.d("'seek' end")
    }

    @JvmName("setVolumeTo")
    fun setVolume(
        to: Float,
        forceFade: Boolean = false,
        onFinish: (Boolean) -> Unit,
    ) {
        Timber.d("'setVolume' start to$to")
        fader?.stop()
        when {
            to == volume -> onFinish(true)
            forceFade || fadePlayback -> {
                fader = Fader(
                    volume, to, fadePlaybackDuration,
                    onUpdate = {
                        setVolumeInstant(it)
                    },
                    onFinish = {
                        onFinish(it)
                        fader = null
                    }
                )
                fader?.start()
            }

            else -> {
                setVolumeInstant(to)
                onFinish(true)
            }
        }
        Timber.d("'setVolume' end")
    }

    fun setVolumeInstant(to: Float) {
        Timber.d("'setVolumeInstant' start")
        volume = to
        mediaPlayer?.setVolume(to, to)
        Timber.d("'setVolumeInstant' end")
    }

    @JvmName("setSpeedTo")
    fun setSpeed(to: Float) {
        Timber.d("'setSpeed' start")
        if (!hasPlayedOnce) {
            speed = to
            return
        }
        mediaPlayer?.let {
            val isPlaying = it.isPlaying
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.playbackParams = it.playbackParams.setSpeed(to)
                }
                speed = to
            } catch (err: Exception) {
                Log.e("RadioPlayer", "changing speed failed", err)
            }
            if (!isPlaying) {
                it.pause()
            }
        }
        Timber.d("'setSpeed' end")

    }

    @JvmName("setPitchTo")
    fun setPitch(to: Float) {
        Timber.d("'setPitch' start")
        if (!hasPlayedOnce) {
            pitch = to
            return
        }
        mediaPlayer?.let {
            val isPlaying = it.isPlaying
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.playbackParams = it.playbackParams.setPitch(to)
                }
                pitch = to
            } catch (err: Exception) {
                Log.e("RadioPlayer", "changing pitch failed", err)
            }
            if (!isPlaying) {
                it.pause()
            }
        }
        Timber.d("'setPitch' end")
    }

    fun setOnPlaybackPositionListener(listener: RadioPlayerOnPlaybackPositionListener?) {
        onPlaybackPosition = listener
    }

    private fun createDurationTimer() {
        playbackPositionUpdater = kotlin.concurrent.timer(period = 100L) {
            playbackPosition?.let {
                onPlaybackPosition?.invoke(it)
            }
        }
    }

    private fun destroyDurationTimer() {
        playbackPositionUpdater?.cancel()
        playbackPositionUpdater = null
    }

    companion object {
        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
        const val DUCK_VOLUME = 0.2f
        const val DEFAULT_SPEED = 1f
        const val DEFAULT_PITCH = 1f
    }
}
