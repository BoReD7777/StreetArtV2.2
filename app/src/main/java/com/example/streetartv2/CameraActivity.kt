package com.example.streetartv2

import Artwork
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraActivity : AppCompatActivity() {

    // ... wszystkie Twoje zmienne i launchery pozostają bez zmian ...
    private lateinit var drawingView: DrawingView
    private lateinit var buttonClear: ImageButton
    private lateinit var buttonSave: ImageButton
    private lateinit var buttonUndo: ImageButton
    private lateinit var buttonChangeColor: ImageButton
    private lateinit var buttonPen: Button
    private lateinit var buttonMarker: Button
    private lateinit var buttonSpray: Button
    private lateinit var buttonCrayon: Button
    private lateinit var buttonNeon: Button
    private lateinit var buttonDashed: Button
    private lateinit var buttonTakePhoto: Button
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var brushPanel: RelativeLayout
    private lateinit var brushScrollView: HorizontalScrollView
    private lateinit var arrowLeft: ImageButton
    private lateinit var arrowRight: ImageButton
    private lateinit var buttonBack: ImageButton
    private var photoUri: Uri? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentColor: Int = Color.RED
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                checkLocationPermissionAndSave()
            } else {
                Toast.makeText(this, "Brak pozwolenia na lokalizację. Zapisywanie bez niej.", Toast.LENGTH_LONG).show()
                saveDrawing(null)
            }
        }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(this, "Dostęp do aparatu jest wymagany!", Toast.LENGTH_LONG).show()
            }
        }
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                photoUri?.let { uri ->
                    try {
                        val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                        drawingView.setBitmap(imageBitmap)
                        buttonTakePhoto.visibility = View.GONE
                        buttonClear.visibility = View.VISIBLE
                        buttonUndo.visibility = View.VISIBLE
                        buttonSave.visibility = View.VISIBLE
                        buttonChangeColor.visibility = View.VISIBLE
                        sizeSeekBar.visibility = View.VISIBLE
                        brushPanel.visibility = View.VISIBLE
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Nie udało się wczytać zdjęcia", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Anulowano robienie zdjęcia.", Toast.LENGTH_SHORT).show()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Inicjalizacja wszystkich widoków (findViewById)
        drawingView = findViewById(R.id.drawingView)
        sizeSeekBar = findViewById(R.id.sizeSeekBar)
        brushPanel = findViewById(R.id.brushPanel)
        brushScrollView = findViewById(R.id.brushScrollView)
        arrowLeft = findViewById(R.id.arrowLeft)
        arrowRight = findViewById(R.id.arrowRight)
        buttonPen = findViewById(R.id.buttonPen)
        buttonMarker = findViewById(R.id.buttonMarker)
        buttonSpray = findViewById(R.id.buttonSpray)
        buttonCrayon = findViewById(R.id.buttonCrayon)
        buttonNeon = findViewById(R.id.buttonNeon)
        buttonDashed = findViewById(R.id.buttonDashed)
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        buttonClear = findViewById(R.id.buttonClear)
        buttonSave = findViewById(R.id.buttonSave)
        buttonUndo = findViewById(R.id.buttonUndo)
        buttonChangeColor = findViewById(R.id.buttonChangeColor)
        buttonBack = findViewById(R.id.buttonBack)

        // Ustawienie listenerów
        buttonSave.setOnClickListener { checkLocationPermissionAndSave() }
        buttonTakePhoto.setOnClickListener { checkCameraPermissionAndLaunch() }
        buttonClear.setOnClickListener { drawingView.clearDrawing() }
        buttonUndo.setOnClickListener { drawingView.undo() }
        buttonChangeColor.setOnClickListener { openColorPicker() }

        buttonPen.setOnClickListener { drawingView.setBrushType(DrawingView.BrushType.NORMAL) }
        buttonMarker.setOnClickListener { drawingView.setBrushType(DrawingView.BrushType.MARKER) }
        buttonSpray.setOnClickListener { drawingView.setBrushType(DrawingView.BrushType.SPRAY) }
        buttonCrayon.setOnClickListener { drawingView.setBrushType(DrawingView.BrushType.CRAYON) }
        buttonNeon.setOnClickListener { drawingView.setBrushType(DrawingView.BrushType.NEON) }
        buttonDashed.setOnClickListener { drawingView.setBrushType(DrawingView.BrushType.DASHED) }

        arrowLeft.setOnClickListener {
            val maxScroll = (brushScrollView.getChildAt(0) as View).measuredWidth - brushScrollView.measuredWidth
            if (brushScrollView.scrollX < 50) {
                brushScrollView.smoothScrollTo(maxScroll, 0)
            } else {
                brushScrollView.smoothScrollBy(-200, 0)
            }
        }

        arrowRight.setOnClickListener {
            val maxScroll = (brushScrollView.getChildAt(0) as View).measuredWidth - brushScrollView.measuredWidth
            if (brushScrollView.scrollX >= maxScroll - 50) {
                brushScrollView.smoothScrollTo(0, 0)
            } else {
                brushScrollView.smoothScrollBy(200, 0)
            }
        }

        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                drawingView.setBrushSize(progress + 5f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        buttonBack.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Powrót do menu")
                .setMessage("Czy na pewno chcesz wyjść? Niezapisane zmiany zostaną utracone.")
                .setPositiveButton("Tak, wyjdź") { _, _ -> finish() }
                .setNegativeButton("Anuluj", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }


    @SuppressLint("MissingPermission")
    private fun checkLocationPermissionAndSave() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? -> saveDrawing(location) }
                .addOnFailureListener {
                    Toast.makeText(this, "Nie udało się pobrać lokalizacji.", Toast.LENGTH_SHORT).show()
                    saveDrawing(null)
                }
        } else {
            requestLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // GŁÓWNA ZAKTUALIZOWANA FUNKCJA
    private fun saveDrawing(location: Location?) {
        Toast.makeText(this, "Zapisywanie...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Odczytujemy nick z SharedPreferences
                val sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE)
                val authorName = sharedPrefs.getString("USER_NICKNAME", "Anonim")

                // 2. Pobieramy gotowy obrazek z DrawingView
                val bitmap = drawingView.canvasBitmap
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val byteArray = stream.toByteArray()
                val fileName = "artwork_${System.currentTimeMillis()}.jpg"

                // 3. Wysyłamy plik do Supabase Storage
                StreetArtApplication.supabase.storage.from("artworks").upload(fileName, byteArray, upsert = true)

                // 4. Pobieramy publiczny URL do wysłanego pliku
                val publicUrl = StreetArtApplication.supabase.storage.from("artworks").publicUrl(fileName)

                // 5. Pobieramy czytelny adres (jeśli jest lokalizacja)
                val addressText: String? = location?.let {
                    try {
                        val geocoder = Geocoder(this@CameraActivity, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val address = addresses[0]
                            listOfNotNull(address.locality, address.thoroughfare).joinToString(", ")
                        } else { null }
                    } catch (e: IOException) { null }
                }

                // 6. Tworzymy obiekt Artwork z WSZYSTKIMI danymi

                val artwork = Artwork(
                    imageUrl = publicUrl,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    address = addressText
                )

                // 7. Wstawiamy obiekt do bazy danych
                StreetArtApplication.supabase.from("artworks").insert(artwork)

                // Po wszystkim, wracamy do wątku UI, aby pokazać sukces i zamknąć ekran
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CameraActivity, "Zapisano i wysłano do chmury!", Toast.LENGTH_LONG).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("CameraActivity_Save", "Błąd podczas zapisu", e)
                    Toast.makeText(this@CameraActivity, "Błąd zapisu: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ... reszta metod (openColorPicker, checkCameraPermissionAndLaunch itd.) pozostaje bez zmian ...
    private fun openColorPicker() {
        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Wybierz kolor")
            .initialColor(currentColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(12)
            .setPositiveButton("OK") { _, selectedColor, _ ->
                currentColor = selectedColor
                drawingView.setBrushColor(selectedColor)
            }
            .setNegativeButton("Anuluj", null)
            .build()
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val file = createImageFile()
            photoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
            photoUri?.let { cameraLauncher.launch(it) }
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(this, "Błąd podczas tworzenia pliku na zdjęcie", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
}