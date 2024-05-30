package meloplayer.app.interfaces

import meloplayer.app.model.Album
import meloplayer.app.model.Artist
import meloplayer.app.model.Genre

interface IHomeClickListener {
    fun onAlbumClick(album: Album)

    fun onArtistClick(artist: Artist)

    fun onGenreClick(genre: Genre)
}