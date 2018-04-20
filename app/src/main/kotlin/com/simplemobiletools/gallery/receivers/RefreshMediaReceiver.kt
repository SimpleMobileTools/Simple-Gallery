package com.simplemobiletools.gallery.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.gallery.asynctasks.GetDirectoriesAsynctask
import com.simplemobiletools.gallery.extensions.storeDirectoryItems

class RefreshMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GetDirectoriesAsynctask(context, false, false) {
            context.storeDirectoryItems(it)
        }.execute()
    }
}
