package com.example.streetartv2

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ArtworkDetailActivity : AppCompatActivity() {

    private lateinit var detailImageView: ImageView
    private lateinit var detailAddressTextView: TextView
    private lateinit var detailCoordsTextView: TextView
    private lateinit var detailDateTextView: TextView
    // --- NOWY PRZYCISK ---
    private lateinit var deleteButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artwork_detail)

        detailImageView = findViewById(R.id.detailImageView)
        detailAddressTextView = findViewById(R.id.detailAddressTextView)
        detailCoordsTextView = findViewById(R.id.detailCoordsTextView)
        detailDateTextView = findViewById(R.id.detailDateTextView)
        deleteButton = findViewById(R.id.deleteButton) // Inicjalizacja przycisku

        val artworkJson = intent.getStringExtra("ARTWORK_EXTRA")

        if (artworkJson != null) {
            val json = Json { ignoreUnknownKeys = true }
            val artwork = json.decodeFromString<Artwork>(artworkJson)
            bindData(artwork)
        } else {
            Toast.makeText(this, "Błąd wczytywania danych dzieła.", Toast.LENGTH_LONG).show()
            finish() // Zamykamy ekran, jeśli nie ma danych
        }
    }

    private fun bindData(artwork: Artwork) {
        detailImageView.load(artwork.imageUrl) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.holo_red_dark)
        }

        detailAddressTextView.text = artwork.address ?: "Brak adresu"

        if (artwork.latitude != null && artwork.longitude != null) {
            val coordsText = "Szer: ${"%.4f".format(Locale.US, artwork.latitude)}, Dł: ${"%.4f".format(Locale.US, artwork.longitude)}"
            detailCoordsTextView.text = coordsText
        } else {
            detailCoordsTextView.text = "Brak danych o współrzędnych"
        }

        detailDateTextView.text = artwork.createdAt?.let { "Dodano: ${formatIsoDate(it)}" } ?: "Brak daty"

        // --- NOWA LOGIKA: SPRAWDZANIE WŁASNOŚCI I POKAZYWANIE PRZYCISKU ---
        val sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE)
        val currentUserNickname = sharedPrefs.getString("USER_NICKNAME", null)

        if (currentUserNickname != null && currentUserNickname == artwork.authorUsername) {
            deleteButton.visibility = View.VISIBLE
            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(artwork)
            }
        }
    }

    // --- NOWA METODA: Pokaż okno dialogowe z potwierdzeniem ---
    private fun showDeleteConfirmationDialog(artwork: Artwork) {
        AlertDialog.Builder(this)
            .setTitle("Potwierdź usunięcie")
            .setMessage("Czy na pewno chcesz usunąć to dzieło? Ta operacja jest nieodwracalna.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Tak, usuń") { _, _ ->
                deleteArtworkFromSupabase(artwork)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    // --- NOWA METODA: Usuwa dzieło z Supabase ---
    private fun deleteArtworkFromSupabase(artwork: Artwork) {
        lifecycleScope.launch {
            try {
                // Krok 1: Usuń wpis z bazy danych
                StreetArtApplication.supabase.from("artworks").delete {
                    filter {
                        artwork.id?.let { eq("id", it) }
                    }
                }

                // Krok 2: Usuń plik z Supabase Storage
                // Wyciągamy nazwę pliku z jego pełnego URL
                val fileName = artwork.imageUrl.substring(artwork.imageUrl.lastIndexOf('/') + 1)
                StreetArtApplication.supabase.storage.from("artworks").delete(fileName)

                // Wróć do wątku UI, aby pokazać sukces i zamknąć ekran
                runOnUiThread {
                    Toast.makeText(this@ArtworkDetailActivity, "Usunięto dzieło.", Toast.LENGTH_SHORT).show()
                    finish() // Zamknij ekran szczegółów i wróć do galerii
                }

            } catch (e: Exception) {
                Log.e("DetailActivity", "Błąd podczas usuwania", e)
                runOnUiThread {
                    Toast.makeText(this@ArtworkDetailActivity, "Błąd usuwania: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun formatIsoDate(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(isoDate)
            val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
            formatter.timeZone = TimeZone.getDefault()
            date?.let { formatter.format(it) } ?: "Błędna data"
        } catch (e: Exception) { isoDate }
    }
}