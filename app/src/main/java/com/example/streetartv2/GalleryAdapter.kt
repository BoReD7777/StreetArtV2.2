package com.example.streetartv2

import Artwork
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GalleryAdapter(private var artworks: List<Artwork>) :
    RecyclerView.Adapter<GalleryAdapter.ArtworkViewHolder>() {

    // ZMIANA: Dodajemy referencję do nowego TextView
    class ArtworkViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.artworkImageView)
        val addressView: TextView = view.findViewById(R.id.addressTextView)
        val authorView: TextView = view.findViewById(R.id.authorTextView) // <-- NOWA ZMIENNA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtworkViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gallery_item, parent, false)
        return ArtworkViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArtworkViewHolder, position: Int) {
        val artwork = artworks[position]

        holder.imageView.load(artwork.imageUrl) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }

        holder.addressView.text = if (!artwork.address.isNullOrBlank()) {
            artwork.address
        } else {
            "Brak danych o lokalizacji"
        }

        // --- NOWA LOGIKA: Ustawiamy tekst dla nicku autora ---
        holder.authorView.text = if (!artwork.authorUsername.isNullOrBlank()) {
            "Autor: ${artwork.authorUsername}"
        } else {
            "Autor: Anonim"
        }
        // --- KONIEC NOWEJ LOGIKI ---

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ArtworkDetailActivity::class.java).apply {
                val artworkJson = Json.encodeToString(artwork)
                putExtra("ARTWORK_EXTRA", artworkJson)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = artworks.size

    fun updateData(newArtworks: List<Artwork>) {
        this.artworks = newArtworks
        notifyDataSetChanged()
    }
}