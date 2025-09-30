package com.simplemobiletools.gallery.pro

import android.app.Application
import com.github.ajalt.reprint.core.Reprint
import com.simplemobiletools.commons.extensions.checkUseEnglish
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import com.simplemobiletools.gallery.pro.extensions.favoritesDB
import com.simplemobiletools.gallery.pro.helpers.supabase
import com.simplemobiletools.gallery.pro.models.Favorite
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Response

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
        Reprint.initialize(this)
        Picasso.setSingletonInstance(Picasso.Builder(this).downloader(object : Downloader {
            override fun load(request: Request) = Response.Builder().build()

            override fun shutdown() {}
        }).build())

        // Fetches favorites from Supabase on startup.
        // This is a simple one-way sync that overwrites the local database.
        // A more robust two-way sync would be needed for a production environment.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val favorites = supabase.postgrest["favorites"].select().decodeList<Favorite>()
                favoritesDB.insertAll(favorites)
            } catch (e: Exception) {
                Log.e("Supabase", "Error fetching favorites", e)
            }
        }
    }
}
