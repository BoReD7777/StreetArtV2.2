package com.example.streetartv2

import Artwork
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// ZMIANA: Implementujemy nowy interfejs do obsługi kliknięć w pinezki
class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var filterSwitch: SwitchCompat
    private lateinit var radiusSpinner: Spinner
    private var currentUserLocation: Location? = null
    private var currentRadiusInMeters: Int = 50
    private var unlockedArtworks: List<Artwork> = emptyList() // Lista odblokowanych prac

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showUserLocationAndFetchArtworks()
            } else {
                Toast.makeText(this, "Zgoda na lokalizację jest wymagana.", Toast.LENGTH_LONG).show()
                fetchAndDisplayArtworks(isNearbyFilterEnabled = false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        filterSwitch = findViewById(R.id.filterSwitch)
        radiusSpinner = findViewById(R.id.radiusSpinner)

        setupSpinner()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        filterSwitch.setOnCheckedChangeListener { _, isChecked ->
            radiusSpinner.visibility = if (isChecked) View.VISIBLE else View.GONE
            fetchAndDisplayArtworks(isChecked)
        }
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(this, R.array.radius_options, android.R.layout.simple_spinner_item)
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                radiusSpinner.adapter = adapter
            }
        radiusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentRadiusInMeters = when (position) { 0 -> 50; 1 -> 200; 2 -> 1000; 3 -> 5000; else -> 50 }
                if (filterSwitch.isChecked) { fetchAndDisplayArtworks(true) }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Mamy pozwolenie, więc możemy kontynuować
            showUserLocationAndFetchArtworks()
        } else {
            // Nie mamy pozwolenia, więc o nie prosimy
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // --- NOWOŚĆ: Ustawiamy listener kliknięć w pinezki ---
        mMap.setOnMarkerClickListener(this)
        checkLocationPermission()
    }

    // --- NOWA METODA: Wywoływana po kliknięciu w pinezkę ---
    @SuppressLint("MissingPermission") // Dodajemy adnotację, bo sprawdzamy uprawnienia ręcznie
    override fun onMarkerClick(marker: Marker): Boolean {
        // Pobieramy obiekt Artwork, który "schowaliśmy" w pinezce
        val clickedArtwork = marker.tag as? Artwork ?: return false

        val sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE)
        val currentUserNickname = sharedPrefs.getString("USER_NICKNAME", null)

        // Jeśli to nasza praca, nic nie rób (pokaż domyślne okienko z tytułem)
        if (clickedArtwork.authorUsername == currentUserNickname) {
            return false // Zwrócenie false powoduje domyślne zachowanie
        }

        // --- NOWA, POPRAWIONA LOGIKA ---
        // Sprawdzamy, czy mamy uprawnienia do lokalizacji
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Prosimy o świeżą lokalizację
            fusedLocationClient.lastLocation.addOnSuccessListener { currentLocation: Location? ->
                if (currentLocation == null) {
                    Toast.makeText(this, "Nie można pobrać Twojej aktualnej lokalizacji. Spróbuj ponownie.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Tworzymy obiekt Location dla dzieła sztuki
                val artworkLocation = Location("").apply {
                    latitude = clickedArtwork.latitude!!
                    longitude = clickedArtwork.longitude!!
                }

                // Obliczamy dystans do ŚWIEŻEJ lokalizacji
                val distance = currentLocation.distanceTo(artworkLocation)

                // Wyświetlamy dystans, żeby łatwiej było debugować
                Toast.makeText(this, "Odległość: ${distance.toInt()} metrów", Toast.LENGTH_SHORT).show()

                if (distance < 10) { // Zwiększyłem trochę promień do 25 metrów
                    unlockArtwork(clickedArtwork)
                } else {
                    Toast.makeText(this, "Jesteś za daleko! Podejdź bliżej, aby odblokować.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Nie masz uprawnień do lokalizacji!", Toast.LENGTH_SHORT).show()
        }

        return true // Zwracamy true, bo sami obsłużyliśmy kliknięcie
    }

    private fun fetchAndDisplayArtworks(isNearbyFilterEnabled: Boolean) {
        // ... ta funkcja pozostaje prawie bez zmian, ale w pętli dodajemy .tag do markera
        lifecycleScope.launch {
            try {
                runOnUiThread { mMap.clear() }
                val artworks: List<Artwork>
                if (isNearbyFilterEnabled && currentUserLocation != null) {
                    artworks = StreetArtApplication.supabase.postgrest.rpc(
                        function = "nearby_artworks",
                        parameters = mapOf(
                            "user_lat" to currentUserLocation!!.latitude,
                            "user_long" to currentUserLocation!!.longitude,
                            "radius_meters" to currentRadiusInMeters
                        )
                    ).decodeList<Artwork>()
                } else {
                    artworks = StreetArtApplication.supabase.from("artworks").select().decodeList<Artwork>()
                }

                runOnUiThread {
                    for (artwork in artworks) {
                        if (artwork.latitude != null && artwork.longitude != null) {
                            val position = LatLng(artwork.latitude, artwork.longitude)
                            val marker = mMap.addMarker(
                                MarkerOptions().position(position).title(artwork.address ?: "Street Art")
                            )
                            // --- NOWOŚĆ: "Wkładamy" cały obiekt Artwork do pinezki ---
                            marker?.tag = artwork
                        }
                    }
                    if (artworks.isEmpty() && isNearbyFilterEnabled) {
                        Toast.makeText(this@MapActivity, "W pobliżu nie ma żadnych prac.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MapActivity", "Błąd pobierania dzieł sztuki", e)
                runOnUiThread { Toast.makeText(this@MapActivity, "Nie udało się załadować dzieł na mapę: ${e.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }

    // --- NOWA METODA: Dodaje wpis do tabeli unlocked_artworks ---
    private fun unlockArtwork(artworkToUnlock: Artwork) {
        val sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE)
        val currentUserNickname = sharedPrefs.getString("USER_NICKNAME", null)

        if (currentUserNickname == null) {
            runOnUiThread { Toast.makeText(this, "Błąd: Nie jesteś zalogowany!", Toast.LENGTH_LONG).show() }
            return
        }

        val artworkId = artworkToUnlock.id
        if (artworkId == null) {
            runOnUiThread { Toast.makeText(this, "Błąd: To dzieło nie ma ID!", Toast.LENGTH_LONG).show() }
            return
        }

        // --- NAJWAŻNIEJSZA ZMIANA JEST TUTAJ ---
        // Zamiast tworzyć 'mapOf', tworzymy instancję naszej nowej, bezpiecznej klasy.
        val unlockRecord = UnlockRecord(
            userNickname = currentUserNickname,
            artworkId = artworkId
        )

        lifecycleScope.launch {
            try {
                // Teraz 'insert' dokładnie wie, co ma zrobić.
                StreetArtApplication.supabase.from("unlocked_artworks").insert(unlockRecord)

                Log.d("UnlockArtwork", "Sukces! Zapisano w bazie.")
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "Odblokowano nowe dzieło! Gratulacje!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("UnlockArtwork", "Błąd podczas zapisu do bazy: ${e.message}", e)
                if (e.message?.contains("duplicate key value violates unique constraint") == true) {
                    runOnUiThread {
                        Toast.makeText(this@MapActivity, "Ta praca jest już w Twojej galerii.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@MapActivity, "Błąd odblokowania: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    @Serializable
    data class UnlockRecord(
        @SerialName("user_nickname")
        val userNickname: String,

        @SerialName("artwork_id")
        val artworkId: Int
    )

    // ... reszta klasy bez zmian ...
    @SuppressLint("MissingPermission")
    private fun showUserLocationAndFetchArtworks() {
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                currentUserLocation = location
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17f))
                    fetchAndDisplayArtworks(filterSwitch.isChecked)
                } else {
                    Toast.makeText(this, "Nie można uzyskać lokalizacji. Włącz GPS.", Toast.LENGTH_LONG).show()
                    fetchAndDisplayArtworks(isNearbyFilterEnabled = false)
                }
            }
    }
}