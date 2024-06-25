package meloplayer.app.service.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import meloplayer.app.extensions.uri
import meloplayer.app.model.Song
import meloplayer.app.service.PlaybackManager
import meloplayer.app.service.playback.Playback
import meloplayer.app.service.player.util.AudioEffectSessionManagement
import meloplayer.app.service.player.util.FocusManager
import meloplayer.app.util.PreferenceUtil
import timber.log.Timber

class PlaybackManagerNew(private val context: Context): PlaybackManager {
    var player: SingleMediaPlayerWrapper? = null

    private var restoreOnFocusGain = false
    private val audioEffectSessionManagement = AudioEffectSessionManagement(context)
    private var callbacksTemp: Playback.PlaybackCallbacks? = null
    override fun setCallbacks(callbacks: Playback.PlaybackCallbacks){
        callbacksTemp = callbacks
    }

    private val focusManager = FocusManager(
        context,
        onFocusGain = {
            Timber.d("onFocusGain")
            if (isPlaying) {
                Timber.d("onFocusGain isPlaying:$isPlaying")
                player?.let {

                    Timber.d("onFocusGain isPlaying:$isPlaying")
                    it.setVolume(SingleMediaPlayerWrapper.MAX_VOLUME) {}
                }
            } else {
                play()
            }
        },
        onFocusLoss = {
            Timber.d("onFocusLoss")
            restoreOnFocusGain = isPlaying
            if (!PreferenceUtil.isAudioFocusEnabled) {
                pause()
            }
        },
        onDuck = {
            player?.let {
                it.setVolume(SingleMediaPlayerWrapper.DUCK_VOLUME) {}
            }
        }
    )


    override val audioSessionId: Int?
        get() = player?.audioSessionId
    override val songDurationMillis: Int?
        get() = player?.playbackPosition?.total?.toInt()

    override val songProgressMillis: Int?
        get() = player?.playbackPosition?.played?.toInt()

    override val isPlaying: Boolean
        get() = player?.isPlaying ?: false

    override fun pause(force: Boolean, onFinish: () -> Unit) {
        pause(onFinish)
    }

    fun pause(onFinish: () -> Unit = {}) {
        player?.let {
            if (!it.isPlaying) return@let
            val sessionId = it.audioSessionId
            it.setVolume(
                to = SingleMediaPlayerWrapper.MIN_VOLUME,
            ) { _ ->
                it.pause()
                focusManager.abandonFocus()
                audioEffectSessionManagement.closeAudioEffectSession(sessionId)
                onFinish()
            }
        }
    }


    fun play(){
        play {  }
    }
    override  fun play(onNotInitialized: () -> Unit ) {
        if (player?.usable == null || !player?.usable!!) {
            onNotInitialized()
            return
        }
        player?.let {
            val hasFocus = focusManager.requestFocus()
            if (hasFocus || !PreferenceUtil.isAudioFocusEnabled) {
                it.audioSessionId?.run {
                    audioEffectSessionManagement.openAudioEffectSession(this)
                }
                if (it.fadePlayback) {
                    it.setVolumeInstant(SingleMediaPlayerWrapper.MIN_VOLUME)
                }
                it.setVolume(SingleMediaPlayerWrapper.MAX_VOLUME) {}
                it.start()
            }
        }
    }
    override fun setDataSource(song: Song, force: Boolean, completion: (Boolean) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            try {
                setDataSource(song.uri, force, 0, completion)
            } catch (e: Exception){
                e.printStackTrace()
                completion(false)
            }
        }


    }

    fun setDataSource(
        uri: Uri,
        force: Boolean = false,
        startPosition: Int = 0,
        completion: (success: Boolean) -> Unit = {},
    ) {
        val onFinish = {

            onSongFinish()
            callbacksTemp?.onTrackWentToNext()
        }

        val onError = { what: Int, extra: Int ->
            when {
                // happens when change playback params fail, we skip it since its non-critical
                what == 1 && extra == -22 -> onSongFinish()
                else -> {
                    onSongFinish()
                }
            }
            completion(false)
        }
        val onPrepared = { it: SingleMediaPlayerWrapper ->
            completion(true)
            it.apply {
                if(startPosition != 0){
                    seek(startPosition)
                }
                setSpeed(PreferenceUtil.playbackSpeed)
                setPitch(PreferenceUtil.playbackPitch)
            }
        }

        cleanUpCurrentPlayer()

            player = SingleMediaPlayerWrapper(
                context, uri,
                onFinish = { onFinish() },
                onError = { what, extra ->
                    onError(what, extra)
                },
                onPrepared = {
                    onPrepared(it)
                }
            )

    }


    fun seek(millis: Int) {
        seek(millis, false)
    }

    override fun seek(millis: Int, force: Boolean): Int {
        player?.seek(millis)
        return millis
    }


    private fun onSongFinish() {
        cleanUpCurrentPlayer()
        callbacksTemp?.onTrackEnded()
        nextTrackUriPath?.run {
            Handler(Looper.getMainLooper()).post {
                setDataSource(this)
                play()
            }

        }
    }


    private var nextTrackUriPath: Uri? = null

    override fun setNextDataSource(trackUri: Uri?) {
        nextTrackUriPath = trackUri
        if (trackUri == null) {
            return
        }
    }


    override fun setPlaybackSpeedPitch(playbackSpeed: Float, playbackPitch: Float) {
        player?.apply {
            setSpeed(playbackSpeed)
            setPitch(playbackPitch)
        }
    }

    override fun setCrossFadeDuration(duration: Int) {
        player?.fadePlaybackDuration = duration
    }

    override fun maybeSwitchToCrossFade(crossFadeDuration: Int): Boolean {
        val returnValue = (player == null || (player!!.fadePlayback != crossFadeDuration > 0))
        player?.fadePlaybackDuration = crossFadeDuration
        return returnValue
    }

    private fun cleanUpCurrentPlayer() {
        player?.let {
            //remove playback position update listener
            it.setOnPlaybackPositionListener {}
            //fade out volume
            println("player?.usable")
            it.setVolume(SingleMediaPlayerWrapper.MIN_VOLUME) { _ ->
                //only after fade out complete, do the real cleanup
                it.cleanUpBeforeDestroy()
                //TODO: onUpdate.dispatch(RadioEvents.StopPlaying)
            }
            player = null
        }
    }

    override  fun release() {
        audioEffectSessionManagement.closeAudioEffectSession(audioSessionId)
        focusManager.abandonFocus()
        cleanUpCurrentPlayer()
    }
}