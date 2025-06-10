package com.example.streetartv2

import Artwork
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class GalleryActivity : AppCompatActivity() {

    private lateinit var galleryRecyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private var artworksList = mutableListOf<Artwork>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        galleryRecyclerView = findViewById(R.id.galleryRecyclerView)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)

        setupRecyclerView()
        fetchArtworksFromSupabase()
    }

    private fun setupRecyclerView() {
        galleryRecyclerView.layoutManager = LinearLayoutManager(this)
        galleryAdapter = GalleryAdapter(artworksList)
        galleryRecyclerView.adapter = galleryAdapter
    }

    private fun fetchArtworksFromSupabase() {
        progressBar.visibility = View.VISIBLE
        galleryRecyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE

        // Odczytujemy zapisany nick użytkownika
        val sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE)
        val currentUserNickname = sharedPrefs.getString("USER_NICKNAME", null)

        // Jeśli z jakiegoś powodu nie ma nicku, nie możemy nic pobrać
        if (currentUserNickname == null) {
            progressBar.visibility = View.GONE
            emptyView.text = "Błąd: Brak zdefiniowanego użytkownika."
            emptyView.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            try {
                // --- ZMIANA: Wywołujemy naszą nową funkcję RPC ---
                val response = StreetArtApplication.supabase.postgrest.rpc(
                    function = "get_visible_artworks",
                    parameters = mapOf("p_user_nickname" to currentUserNickname)
                ).decodeList<Artwork>()


                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (response.isEmpty()) {
                        emptyView.text = "Twoja galeria jest pusta. Stwórz coś!"
                        emptyView.visibility = View.VISIBLE
                        galleryRecyclerView.visibility = View.GONE
                    } else {
                        (galleryRecyclerView.adapter as GalleryAdapter).updateData(response)
                        emptyView.visibility = View.GONE
                        galleryRecyclerView.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e("GalleryActivity", "Błąd pobierania danych z Supabase", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    emptyView.text = "Błąd ładowania galerii."
                    emptyView.visibility = View.VISIBLE
                    Toast.makeText(this@GalleryActivity, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}