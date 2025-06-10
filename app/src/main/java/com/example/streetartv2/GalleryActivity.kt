package com.example.streetartv2

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
import io.github.jan.supabase.postgrest.query.Order
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

        lifecycleScope.launch {
            try {
                // --- POPRAWNA I OSTATECZNA METODA POBIERANIA I DEKODOWANIA DANYCH ---
                val response = StreetArtApplication.supabase.from("artworks")
                    .select {
                        order("created_at", Order.DESCENDING) // Sortujemy od najnowszych
                    }
                    .decodeList<Artwork>() // Ta metoda pobiera i od razu konwertuje JSON na listę obiektów Artwork

                // Po pobraniu danych, wracamy do głównego wątku, aby zaktualizować UI
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (response.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        galleryRecyclerView.visibility = View.GONE
                    } else {
                        (galleryRecyclerView.adapter as GalleryAdapter).updateData(response)
                        emptyView.visibility = View.GONE
                        galleryRecyclerView.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                // Ta sekcja złapie błędy sieciowe lub błędy parsowania, jeśli jeszcze jakieś będą
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