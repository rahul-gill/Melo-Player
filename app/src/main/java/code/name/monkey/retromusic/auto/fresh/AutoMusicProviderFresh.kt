package code.name.monkey.retromusic.auto.fresh

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.auto.AutoMediaIDHelper
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.CategoryInfo
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.AlbumRepository
import code.name.monkey.retromusic.repository.ArtistRepository
import code.name.monkey.retromusic.repository.GenreRepository
import code.name.monkey.retromusic.repository.PlaylistRepository
import code.name.monkey.retromusic.repository.SongRepository
import code.name.monkey.retromusic.repository.TopPlayedRepository
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference


class AutoMusicProviderFresh(
    private val mContext: Context,
    private val songsRepository: SongRepository,
    private val albumsRepository: AlbumRepository,
    private val artistsRepository: ArtistRepository,
    private val genresRepository: GenreRepository,
    private val playlistsRepository: PlaylistRepository,
    private val topPlayedRepository: TopPlayedRepository
) {
    private var mMusicService: WeakReference<MusicService>? = null

    fun setMusicService(service: MusicService) {
        mMusicService = WeakReference(service)
    }

    fun getChildren(mediaId: String?, resources: Resources): List<MediaItem> {
        val mediaItems: MutableList<MediaItem> = ArrayList()
        when (mediaId) {
            AutoMediaIDHelper.MEDIA_ID_ROOT -> {
                mediaItems.addAll(getRootChildren(resources))
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST -> for (playlist in playlistsRepository.playlists()) {
                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId(
                            AutoMediaIDHelper.createMediaID(
                                playlist.id.toString(),
                                mediaId
                            )
                        )
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(playlist.name)
                                .setIsPlayable(true)
                                .setSubtitle(playlist.getInfoString(mContext))
                                .setArtworkDrawable(R.drawable.ic_playlist_play)
                                .build()
                        ).build()
                )
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM -> for (album in albumsRepository.albums()) {
                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId(AutoMediaIDHelper.createMediaID(album.id.toString(), mediaId))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(album.title)
                                .setIsPlayable(true)
                                .setSubtitle(album.albumArtist ?: album.artistName)
                                .setArtworkUri(MusicUtil.getMediaStoreAlbumCoverUri(album.id))
                                .build()
                        ).build()
                )
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST -> for (artist in artistsRepository.artists()) {
                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId(AutoMediaIDHelper.createMediaID(artist.id.toString(), mediaId))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(artist.name)
                                .setIsPlayable(true)
                                .build()
                        ).build()
                )
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM_ARTIST -> for (artist in artistsRepository.albumArtists()) {
                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId(
                            AutoMediaIDHelper.createMediaID(
                                artist.safeGetFirstAlbum().id.toString(),
                                mediaId
                            )
                        )
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(artist.name)
                                .setIsPlayable(true)
                                .build()
                        ).build()
                )
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE -> for (genre in genresRepository.genres()) {
                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId(AutoMediaIDHelper.createMediaID(genre.id.toString(), mediaId))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(genre.name)
                                .setIsPlayable(true)
                                .build()
                        ).build()
                )
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE -> mMusicService?.get()?.playingQueue?.let {
                for (song in it) {
                    MediaItem.Builder()
                        .setMediaId(AutoMediaIDHelper.createMediaID(song.id.toString(), mediaId))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setIsPlayable(true)
                                .setSubtitle(song.artistName)
                                .setArtworkUri(MusicUtil.getMediaStoreAlbumCoverUri(song.albumId))
                                .build()
                        ).build()
                }
            }

            else -> {
                getPlaylistChildren(mediaId, mediaItems)
            }
        }
        return mediaItems
    }

    private fun getPlaylistChildren(
        mediaId: String?, mediaItems: MutableList<MediaItem>
    ) {
        val songs = when (mediaId) {
            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS -> {
                topPlayedRepository.topTracks()
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY -> {
                topPlayedRepository.recentlyPlayedTracks()
            }

            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SUGGESTIONS -> {
                topPlayedRepository.notRecentlyPlayedTracks().take(8)
            }

            else -> {
                emptyList()
            }
        }
        songs.forEach { song ->
            mediaItems.add(
                getPlayableSong(mediaId, song)
            )
        }
    }

    private fun buildMediaItem(
        id: String,
        title: String,
        @DrawableRes artWorkDrawableRes: Int,
        subtitle: String? = null,
        isBrowsable: Boolean = false,
        isPlayable: Boolean = false,
        extras: Bundle? = null
    ): MediaItem {
        val metadataBuilder = MediaMetadata.Builder().setTitle(title)
        if (extras != null) metadataBuilder.setExtras(extras)
        if (subtitle != null) metadataBuilder.setSubtitle(subtitle)
        if (isPlayable) metadataBuilder.setIsPlayable(true)
        if (isBrowsable) metadataBuilder.setIsBrowsable(true)
        metadataBuilder.setArtworkDrawable(artWorkDrawableRes)

        return MediaItem.Builder().setMediaId(id)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun MediaMetadata.Builder.setArtworkDrawable(
        artWorkDrawableRes: Int
    ): MediaMetadata.Builder {
        val stream = ByteArrayOutputStream()
        ResourcesCompat.getDrawable(mContext.resources, artWorkDrawableRes, mContext.theme)
            ?.toBitmap()
            ?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        setArtworkData(stream.toByteArray(), MediaMetadata.PICTURE_TYPE_MEDIA)
        return this
    }

    private fun getRootChildren(resources: Resources): List<MediaItem> {
        val mediaItems: MutableList<MediaItem> = ArrayList()
        val libraryCategories = PreferenceUtil.libraryCategory

        libraryCategories.forEach {
            if (!it.visible) {
                return@forEach
            }
            val isGrid = true
            when (it.category) {
                CategoryInfo.Category.Albums -> {
                    val hintExtras = bundleOf(
                        CONTENT_STYLE_SUPPORTED to true,
                        CONTENT_STYLE_BROWSABLE_HINT to
                                if (isGrid) CONTENT_STYLE_GRID_ITEM_HINT_VALUE
                                else CONTENT_STYLE_LIST_ITEM_HINT_VALUE,
                        CONTENT_STYLE_PLAYABLE_HINT to
                                if (isGrid) CONTENT_STYLE_GRID_ITEM_HINT_VALUE
                                else CONTENT_STYLE_LIST_ITEM_HINT_VALUE
                    )
                    mediaItems.add(
                        buildMediaItem(
                            id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM,
                            isBrowsable = true,
                            artWorkDrawableRes = R.drawable.ic_album,
                            title = resources.getString(R.string.albums),
                            extras = hintExtras
                        )
                    )
                }

                CategoryInfo.Category.Artists -> {
                    if (PreferenceUtil.albumArtistsOnly) {
                        mediaItems.add(
                            buildMediaItem(
                                id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM_ARTIST,
                                isBrowsable = true,
                                artWorkDrawableRes = R.drawable.ic_album_artist,
                                title = resources.getString(R.string.album_artist),
                            )
                        )
                    } else {
                        mediaItems.add(
                            buildMediaItem(
                                id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST,
                                isBrowsable = true,
                                artWorkDrawableRes = R.drawable.ic_artist,
                                title = resources.getString(R.string.artists),
                            )
                        )
                    }
                }

                CategoryInfo.Category.Genres -> {
                    mediaItems.add(
                        buildMediaItem(
                            id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE,
                            isBrowsable = true,
                            artWorkDrawableRes = R.drawable.ic_guitar,
                            title = resources.getString(R.string.genres),
                        )
                    )
                }

                CategoryInfo.Category.Playlists -> {
                    mediaItems.add(
                        buildMediaItem(
                            id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST,
                            isBrowsable = true,
                            artWorkDrawableRes = R.drawable.ic_playlist_play,
                            title = resources.getString(R.string.playlists),
                        )
                    )
                }

                else -> {}
            }

        }
        mediaItems.add(
            buildMediaItem(
                id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SHUFFLE,
                title = resources.getString(R.string.action_shuffle_all),
                subtitle = MusicUtil.getPlaylistInfoString(mContext, songsRepository.songs()),
                artWorkDrawableRes = R.drawable.ic_shuffle,
                isPlayable = true
            )
        )
        mediaItems.add(
            buildMediaItem(
                id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE,
                title = resources.getString(R.string.queue),
                subtitle = MusicUtil.getPlaylistInfoString(
                    mContext,
                    MusicPlayerRemote.playingQueue
                ),
                artWorkDrawableRes = R.drawable.ic_queue_music,
                isBrowsable = true
            )
        )
        mediaItems.add(
            buildMediaItem(
                id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS,
                title = resources.getString(R.string.my_top_tracks),
                subtitle = MusicUtil.getPlaylistInfoString(
                    mContext,
                    topPlayedRepository.topTracks()
                ),
                artWorkDrawableRes = R.drawable.ic_trending_up,
                isBrowsable = true
            )
        )
        mediaItems.add(buildMediaItem(
            id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SUGGESTIONS,
            title = resources.getString(R.string.suggestion_songs),
            subtitle = MusicUtil.getPlaylistInfoString(mContext,
                topPlayedRepository.notRecentlyPlayedTracks().takeIf {
                    it.size > 9
                } ?: emptyList()),
            artWorkDrawableRes = R.drawable.ic_face,
            isBrowsable = true
        ))
        mediaItems.add(
            buildMediaItem(
                id = AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY,
                title = resources.getString(R.string.history),
                subtitle = MusicUtil.getPlaylistInfoString(
                    mContext,
                    topPlayedRepository.recentlyPlayedTracks()
                ),
                artWorkDrawableRes = R.drawable.ic_history,
                isBrowsable = true
            )
        )
        return mediaItems
    }

    private fun getPlayableSong(mediaId: String?, song: Song): MediaItem {
        return MediaItem.Builder()
            .setMediaId(AutoMediaIDHelper.createMediaID(song.id.toString(), mediaId))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setSubtitle(song.artistName)
                    .setArtworkUri(MusicUtil.getMediaStoreAlbumCoverUri(song.albumId))
                    .build()
            ).build()
    }

    companion object {

        // Hints - see https://developer.android.com/training/cars/media#default-content-style
        const val CONTENT_STYLE_SUPPORTED = "android.media.browse.CONTENT_STYLE_SUPPORTED"
        const val CONTENT_STYLE_BROWSABLE_HINT = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        const val CONTENT_STYLE_PLAYABLE_HINT = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        const val CONTENT_STYLE_LIST_ITEM_HINT_VALUE = 1
        const val CONTENT_STYLE_GRID_ITEM_HINT_VALUE = 2
    }
}