package com.example.bleattendance.data.supabase

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    // Your actual Supabase project configuration
    const val SUPABASE_URL = "https://dyqmswfkejvnnflmlpxe.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR5cW1zd2ZrZWp2bm5mbG1scHhlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTcwOTAwMDIsImV4cCI6MjA3MjY2NjAwMn0.WJ0KAN0kwi_hRXxQyu14i9FopFUMwh1wPP7erOyjdRU"
    
    init {
        println("🔧 SupabaseConfig initialized")
        println("   - URL: $SUPABASE_URL")
        println("   - Key: ${SUPABASE_ANON_KEY.take(20)}...")
    }
    
    val supabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
