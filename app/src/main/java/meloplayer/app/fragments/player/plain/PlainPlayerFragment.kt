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
package meloplayer.app.fragments.player.plain

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import meloplayer.appthemehelper.util.ToolbarContentTintHelper
import meloplayer.app.R
import meloplayer.app.databinding.FragmentPlainPlayerBinding
import meloplayer.app.extensions.colorControlNormal
import meloplayer.app.extensions.drawAboveSystemBars
import meloplayer.app.extensions.whichFragment
import meloplayer.app.fragments.base.AbsPlayerFragment
import meloplayer.app.fragments.base.goToAlbum
import meloplayer.app.fragments.base.goToArtist
import meloplayer.app.fragments.player.PlayerAlbumCoverFragment
import meloplayer.app.helper.MusicPlayerRemote
import meloplayer.app.model.Song
import meloplayer.app.util.color.MediaNotificationProcessor

class PlainPlayerFragment : AbsPlayerFragment(R.layout.fragment_plain_player) {
    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    private lateinit var plainPlaybackControlsFragment: PlainPlaybackControlsFragment
    private var lastColor: Int = 0
    override val paletteColor: Int
        get() = lastColor
    private var _binding: FragmentPlainPlayerBinding? = null
    private val binding get() = _binding!!


    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.title.text = song.title
        binding.text.text = song.artistName
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateSong()
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            setOnMenuItemClickListener(this@PlainPlayerFragment)
            ToolbarContentTintHelper.colorizeToolbar(
                this,
                colorControlNormal(),
                requireActivity()
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPlainPlayerBinding.bind(view)
        setUpSubFragments()
        setUpPlayerToolbar()
        binding.title.isSelected = true
        binding.text.isSelected = true
        binding.title.setOnClickListener {
            goToAlbum(requireActivity())
        }
        binding.text.setOnClickListener {
            goToArtist(requireActivity())
        }
        playerToolbar().drawAboveSystemBars()
    }

    private fun setUpSubFragments() {
        plainPlaybackControlsFragment = whichFragment(R.id.playbackControlsFragment)
        val playerAlbumCoverFragment: PlayerAlbumCoverFragment =
            whichFragment(R.id.playerAlbumCoverFragment)
        playerAlbumCoverFragment.setCallbacks(this)
    }

    override fun onShow() {
        plainPlaybackControlsFragment.show()
    }

    override fun onHide() {
        plainPlaybackControlsFragment.hide()
    }

    override fun toolbarIconColor() = colorControlNormal()

    override fun onColorChanged(color: MediaNotificationProcessor) {
        plainPlaybackControlsFragment.setColor(color)
        lastColor = color.primaryTextColor
        libraryViewModel.updateColor(color.primaryTextColor)
        ToolbarContentTintHelper.colorizeToolbar(
            binding.playerToolbar,
            colorControlNormal(),
            requireActivity()
        )
    }

    override fun onFavoriteToggled() {
        toggleFavorite(MusicPlayerRemote.currentSong)
    }

    override fun toggleFavorite(song: Song) {
        super.toggleFavorite(song)
        if (song.id == MusicPlayerRemote.currentSong.id) {
            updateIsFavorite()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
