package com.simplemobiletools.gallery.pro.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.AppWidgetTarget
import com.simplemobiletools.commons.extensions.setBackgroundColor
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.MediaActivity
import com.simplemobiletools.gallery.pro.extensions.config
import com.simplemobiletools.gallery.pro.extensions.directoryDB
import com.simplemobiletools.gallery.pro.extensions.getFileSignature
import com.simplemobiletools.gallery.pro.extensions.widgetsDB
import com.simplemobiletools.gallery.pro.models.Widget

class MyWidgetProvider : AppWidgetProvider() {
    private fun setupAppOpenIntent(context: Context, views: RemoteViews, id: Int, widget: Widget) {
        val intent = Intent(context, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, widget.folderPath)
        }

        val pendingIntent = PendingIntent.getActivity(context, widget.widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(id, pendingIntent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Thread {
            context.widgetsDB.getWidgets().forEach {
                val views = RemoteViews(context.packageName, R.layout.widget)
                views.setBackgroundColor(R.id.widget_holder, context.config.widgetBgColor)

                val path = context.directoryDB.getDirectoryThumbnail(it.folderPath)
                val options = RequestOptions()
                        .signature(path!!.getFileSignature())
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                if (context.config.cropThumbnails) options.centerCrop() else options.fitCenter()

                Handler(Looper.getMainLooper()).post {
                    val widgetSize = context.resources.getDimension(R.dimen.widget_initial_width).toInt()
                    val componentName = ComponentName(context, MyWidgetProvider::class.java)
                    val appWidgetTarget = object : AppWidgetTarget(context, widgetSize, widgetSize, R.id.widget_imageview, views, componentName) {}

                    Glide.with(context)
                            .asBitmap()
                            .load(path)
                            .apply(options)
                            .into(appWidgetTarget)

                    setupAppOpenIntent(context, views, R.id.widget_holder, it)
                    appWidgetManager.updateAppWidget(it.widgetId, views)
                }
            }
        }.start()
    }
}
