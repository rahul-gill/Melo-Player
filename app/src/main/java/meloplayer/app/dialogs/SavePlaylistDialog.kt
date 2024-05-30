/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package meloplayer.app.dialogs

import android.app.Dialog
import android.media.MediaScannerConnection
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import meloplayer.appthemehelper.util.VersionUtils
import meloplayer.app.App
import meloplayer.app.EXTRA_PLAYLIST
import meloplayer.app.R
import meloplayer.app.db.PlaylistWithSongs
import meloplayer.app.extensions.colorButtons
import meloplayer.app.extensions.createNewFile
import meloplayer.app.extensions.extraNotNull
import meloplayer.app.extensions.materialDialog
import meloplayer.app.extensions.openDirectoryAccessDialog
import meloplayer.app.helper.M3UWriter
import meloplayer.app.util.PlaylistsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SavePlaylistDialog : DialogFragment() {
    companion object {
        fun create(playlistWithSongs: PlaylistWithSongs): SavePlaylistDialog {
            return SavePlaylistDialog().apply {
                arguments = bundleOf(
                    EXTRA_PLAYLIST to listOf(playlistWithSongs)
                )
            }
        }

        fun create(playlistWithSongs: List<PlaylistWithSongs>): SavePlaylistDialog {
            return SavePlaylistDialog().apply {
                arguments = bundleOf(
                    EXTRA_PLAYLIST to playlistWithSongs
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playlistWithSongs = extraNotNull<List<PlaylistWithSongs>>(EXTRA_PLAYLIST).value
        if (playlistWithSongs.size == 1) {
            handleSinglePlaylistSave(playlistWithSongs.first())
        } else {
            handleMultiPlaylistSave(playlistWithSongs)
        }
    }

    private fun handleMultiPlaylistSave(playlistWithSongs: List<PlaylistWithSongs>) {

        val onFailure = {
            //runBlocking just to show toast on main thread if called from a background coroutine
            runBlocking {
                withContext(Dispatchers.Main){
                    Toast.makeText(App.getContext(), "Something went wrong", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
        if (VersionUtils.hasR()) {
            openDirectoryAccessDialog(
                onResultOk = { uri ->
                    val folder: DocumentFile? = DocumentFile.fromTreeUri(App.getContext(), uri)
                    if (folder == null) {
                        onFailure()
                        return@openDirectoryAccessDialog
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        playlistWithSongs.forEach { item ->
                            try {
                                val newFile =
                                    folder.createFile("audio/mpegurl", item.playlistEntity.playlistName)
                                if (newFile == null) {
                                    onFailure()
                                    return@forEach
                                }
                                App.getContext().contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->

                                        M3UWriter.writeIO(outputStream, item)

                                }
                            } catch (e: Exception){
                                e.printStackTrace()
                                onFailure()
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                App.getContext(),
                                App.getContext().getString(
                                    R.string.saved_multiple_playlists,
                                    playlistWithSongs.size
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                            dismiss()
                        }
                    }
                },
                onFailure = onFailure
            )
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val files = playlistWithSongs.map { PlaylistsUtil.savePlaylistWithSongs(it) }
                MediaScannerConnection.scanFile(
                    requireActivity(),
                    files.map { it.path }.toTypedArray(),
                    null
                ) { _, _ ->
                }
                withContext(Dispatchers.Main) {

                    Toast.makeText(
                        App.getContext(),
                        App.getContext().getString(
                            R.string.saved_multiple_playlists,
                            files.size
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }
            }
        }
    }

    private fun handleSinglePlaylistSave(playlistWithSongs: PlaylistWithSongs) {
        if (VersionUtils.hasR()) {
            createNewFile(
                "audio/mpegurl",
                playlistWithSongs.playlistEntity.playlistName
            ) { outputStream, data ->
                try {
                    if (outputStream != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            M3UWriter.writeIO(
                                outputStream,
                                playlistWithSongs
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    App.getContext(),
                                    App.getContext().getString(
                                        R.string.saved_playlist_to,
                                        data?.lastPathSegment
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                                dismiss()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        App.getContext(),
                        "Something went wrong : " + e.message,
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val file = PlaylistsUtil.savePlaylistWithSongs(playlistWithSongs)
                MediaScannerConnection.scanFile(
                    requireActivity(),
                    arrayOf<String>(file.path),
                    null
                ) { _, _ ->
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        App.getContext(),
                        App.getContext().getString(R.string.saved_playlist_to, file),
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return materialDialog(R.string.save_playlist_title)
            .setView(R.layout.loading)
            .create().colorButtons()
    }
}
