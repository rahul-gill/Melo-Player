package meloplayer.app.interfaces

import android.view.View
import meloplayer.app.db.PlaylistWithSongs

interface IPlaylistClickListener {
    fun onPlaylistClick(playlistWithSongs: PlaylistWithSongs, view: View)
}