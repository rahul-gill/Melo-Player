package code.name.monkey.retromusic.service.fresh

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.se.omapi.Session
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.BuildConfig
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.service.MediaButtonIntentReceiver
import code.name.monkey.retromusic.util.PreferenceUtil

@UnstableApi
class MusicServiceFresh : BaseMusicServiceFresh() {
    private lateinit var session: MediaLibrarySession
    private val exoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }
    private val player by lazy {
        ForwardingPlayer(exoPlayer)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        session

    override fun onCreate() {
        super.onCreate()
        run setupMediaSession@{
            val action = Intent(this, MainActivity::class.java)
            action.putExtra(MainActivity.EXPAND_PANEL, PreferenceUtil.isExpandPanel)
            action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val clickIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    action,
                    PendingIntent.FLAG_UPDATE_CURRENT or if (VersionUtils.hasMarshmallow())
                        PendingIntent.FLAG_IMMUTABLE
                    else 0
                )

            session = MediaLibrarySession.Builder(this, player, SessionCallbacksFresh())
                .setSessionActivity(clickIntent)
                .build()
        }
    }
}