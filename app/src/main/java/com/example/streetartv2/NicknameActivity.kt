package com.example.streetartv2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NicknameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)

        // 1. Łączymy zmienne z widokami z pliku XML
        val nicknameEditText = findViewById<EditText>(R.id.nicknameEditText)
        val enterButton = findViewById<Button>(R.id.enterButton)

        // 2. Ustawiamy listener, który zareaguje na kliknięcie przycisku
        enterButton.setOnClickListener {
            // 3. Pobieramy tekst z pola i usuwamy białe znaki z początku/końca
            val nickname = nicknameEditText.text.toString().trim()

            // 4. Sprawdzamy, czy użytkownik cokolwiek wpisał
            if (nickname.isBlank()) {
                Toast.makeText(this, "Proszę, wpisz swój nick", Toast.LENGTH_SHORT).show()
            } else {
                // 5. ZAPISUJEMY NICK W PAMIĘCI TELEFONU (w naszym "notesiku")
                val sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE)
                val editor = sharedPrefs.edit()
                editor.putString("USER_NICKNAME", nickname)
                editor.apply() // Zatwierdzamy zmiany

                // 6. PRZECHODZIMY DO MENU GŁÓWNEGO
                val intent = Intent(this, MainActivity::class.java)
                // Dodajemy flagi, aby użytkownik nie mógł wrócić do tego ekranu przyciskiem "wstecz"
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
        }
    }
}