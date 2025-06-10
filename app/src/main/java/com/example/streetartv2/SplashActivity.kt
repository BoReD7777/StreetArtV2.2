package com.example.streetartv2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Otwieramy nasz lokalny "notesik" o nazwie "StreetArtPrefs"
        val sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE)

        // Sprawdzamy, czy jest w nim zapisany nick. Jeśli nie ma, zwróci null.
        val savedNickname = sharedPrefs.getString("USER_NICKNAME", null)

        // Decydujemy, który ekran otworzyć
        if (savedNickname.isNullOrBlank()) {
            // Jeśli nie ma nicku, idź do ekranu wpisywania nicku
            startActivity(Intent(this, NicknameActivity::class.java))
        } else {
            // Jeśli nick już jest, idź prosto do menu głównego
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Niezależnie od wyniku, natychmiast zamknij ten ekran "zwrotnicy"
        finish()
    }
}