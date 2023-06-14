package com.simplemobiletools.commons.extensions

import android.widget.SeekBar

fun SeekBar.onSeekBarChangeListener(seekBarChangeListener: (progress: Int) -> Unit) = setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        seekBarChangeListener(progress)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onStopTrackingTouch(seekBar: SeekBar) {}
})
