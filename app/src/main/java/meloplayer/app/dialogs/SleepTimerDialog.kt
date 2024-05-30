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
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import meloplayer.app.R
import meloplayer.app.databinding.DialogSleepTimerBinding
import meloplayer.app.extensions.addAccentColor
import meloplayer.app.extensions.materialDialog
import meloplayer.app.helper.MusicPlayerRemote
import meloplayer.app.util.MusicUtil
import meloplayer.app.util.PreferenceUtil

class SleepTimerDialog : DialogFragment() {

    private var seekArcProgress: Int = 0
    private lateinit var timerUpdater: TimerUpdater
    private lateinit var dialog: AlertDialog

    private var _binding: DialogSleepTimerBinding? = null
    private val binding get() = _binding!!

    private val shouldFinishLastSong: CheckBox get() = binding.shouldFinishLastSong
    private val seekBar: SeekBar get() = binding.seekBar
    private val timerDisplay: TextView get() = binding.timerDisplay

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        timerUpdater = TimerUpdater()
        _binding = DialogSleepTimerBinding.inflate(layoutInflater)

        val finishMusic = PreferenceUtil.isSleepTimerFinishMusic
        shouldFinishLastSong.apply {
            addAccentColor()
            isChecked = finishMusic
        }
        seekBar.apply {
            addAccentColor()
            seekArcProgress = PreferenceUtil.lastSleepTimerValue
            updateTimeDisplayTime()
            progress = seekArcProgress
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (i < 1) {
                    seekBar.progress = 1
                    return
                }
                seekArcProgress = i
                updateTimeDisplayTime()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                PreferenceUtil.lastSleepTimerValue = seekArcProgress
            }
        })

        materialDialog(R.string.action_sleep_timer).apply {
            if (PreferenceUtil.nextSleepTimerElapsedRealTime > System.currentTimeMillis()) {
                seekBar.isVisible = false
                shouldFinishLastSong.isVisible = false
                timerUpdater.start()
                setPositiveButton(android.R.string.ok, null)
                setNegativeButton(R.string.action_cancel) { _, _ ->
                    timerUpdater.cancel()

                    MusicPlayerRemote.musicService?.sleepTimer?.stopTimer()
                    Toast.makeText(
                        requireContext(),
                        requireContext().resources.getString(R.string.sleep_timer_canceled),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                seekBar.isVisible = true
                shouldFinishLastSong.isVisible = true
                setPositiveButton(R.string.action_set) { _, _ ->
                    PreferenceUtil.isSleepTimerFinishMusic = shouldFinishLastSong.isChecked
                    val minutes = seekArcProgress
                    val nextSleepTimerElapsedTime =
                        SystemClock.elapsedRealtime() + minutes * 60 * 1000
                    PreferenceUtil.nextSleepTimerElapsedRealTime = nextSleepTimerElapsedTime.toInt()
                    MusicPlayerRemote.musicService?.sleepTimer?.startTimer(
                        nextSleepTimerElapsedTime,
                        shouldFinishLastSong.isChecked
                    )

                    Toast.makeText(
                        requireContext(),
                        requireContext().resources.getString(R.string.sleep_timer_set, minutes),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            setView(binding.root)
            dialog = create()

        }
        return dialog
    }

    private fun updateTimeDisplayTime() {
        timerDisplay.text = "$seekArcProgress min"
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        timerUpdater.cancel()
        _binding = null
    }

    private inner class TimerUpdater :
        CountDownTimer(
            PreferenceUtil.nextSleepTimerElapsedRealTime - SystemClock.elapsedRealtime(),
            1000
        ) {

        override fun onTick(millisUntilFinished: Long) {
            timerDisplay.text = MusicUtil.getReadableDurationString(millisUntilFinished)
        }

        override fun onFinish() {}
    }
}
