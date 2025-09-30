package com.simplemobiletools.gallery.pro.helpers

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

val supabase = createSupabaseClient(
    supabaseUrl = "https://your-project-url.supabase.co",
    supabaseKey = "your-anon-key"
) {
    install(Postgrest)
}