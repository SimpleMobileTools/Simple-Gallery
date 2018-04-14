package com.simplemobiletools.gallery.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.extensions.config
import com.simplemobiletools.gallery.helpers.SAVE_DIRS_CNT

class RefreshMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GetDirectoriesAsynctask(context, false, false) {
            val subList = it.subList(0, Math.min(SAVE_DIRS_CNT, it.size))
            context.config.directories = Gson().toJson(subList)
        }.execute()
    }
}
