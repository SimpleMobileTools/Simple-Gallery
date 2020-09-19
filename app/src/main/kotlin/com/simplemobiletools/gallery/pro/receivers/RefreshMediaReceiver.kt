package com.simplemobiletools.gallery.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplemobiletools.commons.helpers.REFRESH_PATH
import com.simplemobiletools.gallery.pro.extensions.addPathToDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RefreshMediaReceiver : BroadcastReceiver(), CoroutineScope by CoroutineScope(Dispatchers.Main) {
    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra(REFRESH_PATH) ?: return
        launch { context.addPathToDB(path) }
    }
}
