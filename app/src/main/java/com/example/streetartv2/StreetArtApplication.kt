package com.example.streetartv2

import android.app.Application
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

class StreetArtApplication : Application() {

    companion object {
        lateinit var supabase: SupabaseClient
    }

    override fun onCreate() {
        super.onCreate()

        val supabaseUrl = "https://qxfuclnsgcihbspuhlbx.supabase.co/"
        val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InF4ZnVjbG5zZ2NpaGJzcHVobGJ4Iiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc0OTUwNjMzMywiZXhwIjoyMDY1MDgyMzMzfQ.A7tXA0e4ZpSxTXg5TDxj5fXMD7hhp93-Xiow6q2M7lc"

        supabase = createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        ) {
            // Zostawiamy tylko te moduły, których używamy i które działają
            install(Postgrest)
            install(Storage)
        }
    }
}