package com.example.streetartv2

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.android.gms.maps.model.MarkerOptions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var filterSwitch: SwitchCompat
    private lateinit var radiusSpinner: Spinner
    private var currentUserLocation: Location? = null
    private var currentRadiusInMeters: Int = 50

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
        // Tworzymy adapter, podając mu nasz niestandardowy layout dla widocznego elementu
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item_custom, // <-- NASZ NOWY LAYOUT
            resources.getStringArray(R.array.radius_options)
        )

        // Ustawiamy wygląd dla rozwijanej listy
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_custom) // <-- NASZ DRUGI NOWY LAYOUT
        radiusSpinner.adapter = adapter

        // Listener pozostaje bez zmian
        radiusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentRadiusInMeters = when (position) {
                    0 -> 50; 1 -> 200; 2 -> 1000; 3 -> 5000; else -> 50
                }
                if (filterSwitch.isChecked) {
                    fetchAndDisplayArtworks(true)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            showUserLocationAndFetchArtworks()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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

    private fun fetchAndDisplayArtworks(isNearbyFilterEnabled: Boolean) {
        lifecycleScope.launch {
            try {
                runOnUiThread { mMap.clear() }
                val artworks: List<Artwork>
                if (isNearbyFilterEnabled && currentUserLocation != null) {
                    // --- ZMIANA: Przekazujemy parametry jako obiekt klasy RpcParameters ---
                    val parameters = RpcParameters(
                        userLat = currentUserLocation!!.latitude,
                        userLong = currentUserLocation!!.longitude,
                        radiusMeters = currentRadiusInMeters
                    )
                    artworks = StreetArtApplication.supabase.postgrest.rpc(
                        function = "nearby_artworks",
                        parameters = parameters // Przekazujemy cały obiekt
                    ).decodeList<Artwork>()
                } else {
                    artworks = StreetArtApplication.supabase.from("artworks").select().decodeList<Artwork>()
                }

                runOnUiThread {
                    for (artwork in artworks) {
                        if (artwork.latitude != null && artwork.longitude != null) {
                            val position = LatLng(artwork.latitude, artwork.longitude)
                            mMap.addMarker(MarkerOptions().position(position).title(artwork.address ?: "Street Art"))
                        }
                    }
                    if (artworks.isEmpty() && isNearbyFilterEnabled) {
                        Toast.makeText(this@MapActivity, "W pobliżu nie ma żadnych prac.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MapActivity", "Złapano błąd przy pobieraniu prac!", e)
                runOnUiThread {
                    Toast.makeText(this@MapActivity, "Wystąpił błąd ładowania prac.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}