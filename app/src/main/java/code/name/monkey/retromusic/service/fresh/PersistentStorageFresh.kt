package code.name.monkey.retromusic.service.fresh

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import code.name.monkey.retromusic.extensions.albumArtUri
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.PersistentStorage

class PersistentStorageFresh(context: Context) {

    private val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveSong(song: Song) {
        prefs.edit {
            putLong("song_id", song.id)
            putString("song_title", song.title)
            putString("song_artist", song.artistName)
            putString("song_cover", song.albumArtUri.toString())
        }
    }

    fun recentSong(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(prefs.getLong("song_id", 0L).toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(prefs.getString("song_title", ""))
                    .setSubtitle(prefs.getString("song_artist", ""))
                    .setArtworkUri(prefs.getString("song_cover", "")?.toUri())
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    companion object {
        const val PREFERENCES_NAME = "retro_recent"

        @Volatile
        private var instance: PersistentStorageFresh? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PersistentStorageFresh(context).also { instance = it }
            }
    }
}