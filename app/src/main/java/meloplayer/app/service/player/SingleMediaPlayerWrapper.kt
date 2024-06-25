package meloplayer.app.service.player

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import meloplayer.app.service.player.util.Fader
import meloplayer.app.util.PreferenceUtil
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

class SingleMediaPlayerWrapper @OptIn(UnstableApi::class)
constructor(
    private val context: Context,
    uri: Uri,
    var onFinish: RadioPlayerOnFinishListener? = null,
     var onError: RadioPlayerOnErrorListener? = null,
     var onPrepared: RadioPlayerOnPreparedListener? = null
) {
    var usable = false
    var hasPlayedOnce = false

    private val unsafeMediaPlayer: ExoPlayer
    private val mediaPlayer: ExoPlayer?
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
        @OptIn(UnstableApi::class)
        get() = mediaPlayer?.audioSessionId
        @OptIn(UnstableApi::class)
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

        unsafeMediaPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            }).build().also { ump ->
                ump.addListener(object: Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when(playbackState){
                            Player.STATE_READY -> {

                                Handler(Looper.getMainLooper()).post {
                                    usable = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        //TODO: what to do about it ump.playbackParams.setAudioFallbackMode(PlaybackParams.AUDIO_FALLBACK_MODE_DEFAULT)
                                    }
                                    createDurationTimer()
                                    onPrepared?.invoke(this@SingleMediaPlayerWrapper)
                                }
                            }
                            Player.STATE_ENDED -> {
                                Handler(Looper.getMainLooper()).post {
                                    usable = false
                                    onFinish?.invoke()
                                }
                            }

                            Player.STATE_BUFFERING -> {
                                //TODO()
                            }

                            Player.STATE_IDLE -> {
                                //TODO()
                            }
                        }
                    }

                    override fun onEvents(player: Player, events: Player.Events) {
                        if (events.contains(Player.EVENT_PLAYER_ERROR)) {
                            Handler(Looper.getMainLooper()).post {
                                usable = false
                                onError?.invoke(-1, -1)
                            }
                        }
                    }
                })
                ump.setMediaItem(MediaItem.fromUri(uri))
                ump.prepare()


//            ump.setOnPreparedListener {
//
//            }
//            ump.setOnCompletionListener {
//
//            }
//            ump.setOnErrorListener { _, what, extra ->
//
//            }
//            ump.setDataSource(context, uri)
//            ump.prepareAsync()
        }
    }


    fun cleanUpBeforeDestroy() {
        Handler(Looper.getMainLooper()).post {
            Timber.d("cleanUpBeforeDestroy start")
            usable = false
            destroyDurationTimer()
            unsafeMediaPlayer.stop()
            //unsafeMediaPlayer.reset()
            unsafeMediaPlayer.release()
            Timber.d("cleanUpBeforeDestroy end")

        }
    }

    fun start(){

        Handler(Looper.getMainLooper()).post {
            Timber.d("'start' start")
            mediaPlayer?.let {
                it.play()
                //it.start()
                if (!hasPlayedOnce) {
                    hasPlayedOnce = true
                    setSpeed(speed)
                    setPitch(pitch)
                }
            }
            Timber.d("'start' end")
        }
    }

    fun pause() = Handler(Looper.getMainLooper()).post{
        Timber.d("'pause' start")
        mediaPlayer?.pause()
        Timber.d("'pause' end")
    }
    fun seek(to: Int) = Handler(Looper.getMainLooper()).post{
        Timber.d("'seek' start")
        mediaPlayer?.seekTo(to.toLong())
        Timber.d("'seek' end")
    }

    @JvmName("setVolumeTo")
    fun setVolume(
        to: Float,
        forceFade: Boolean = false,
        onFinish: (Boolean) -> Unit,
    )  = Handler(Looper.getMainLooper()).post{
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

    fun setVolumeInstant(to: Float) =Handler(Looper.getMainLooper()).post {
        Timber.d("'setVolumeInstant' start")
        volume = to
        mediaPlayer?.volume = to
        Timber.d("'setVolumeInstant' end")
    }

    @JvmName("setSpeedTo")
    fun setSpeed(to: Float) = Handler(Looper.getMainLooper()).post{
        Timber.d("'setSpeed' start")
        if (!hasPlayedOnce) {
            speed = to
            return@post
        }
        mediaPlayer?.let {
            val isPlaying = it.isPlaying
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.playbackParameters = it.playbackParameters.withSpeed(to)
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
    fun setPitch(to: Float) =Handler(Looper.getMainLooper()).post{
        Timber.d("'setPitch' start")
        if (!hasPlayedOnce) {
            pitch = to
            return@post
        }
        mediaPlayer?.let {
            val isPlaying = it.isPlaying
            try {
                it.playbackParameters =
                    PlaybackParameters(it.playbackParameters.speed, to)
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

    internal fun createDurationTimer() {
        playbackPositionUpdater = kotlin.concurrent.timer(period = 100L) {
            playbackPosition?.let {
                Handler(Looper.getMainLooper()).post{
                    onPlaybackPosition?.invoke(it)

                }
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
