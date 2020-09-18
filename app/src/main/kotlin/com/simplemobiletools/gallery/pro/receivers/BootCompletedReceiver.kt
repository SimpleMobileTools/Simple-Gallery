package com.simplemobiletools.gallery.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.gallery.pro.extensions.updateDirectoryPath
import com.simplemobiletools.gallery.pro.helpers.MediaFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    override fun onReceive(context: Context, intent: Intent) {
        launch {
            MediaFetcher(context).getFoldersToScan().forEach {
                context.updateDirectoryPath(it)
            }
        }
    }
}
