package com.example.streetartv2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout; // Nowy import
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Zmiana z RelativeLayout na CoordinatorLayout
    private CoordinatorLayout mainLayout;
    private Button buttonCreate;
    private Button buttonGallery;
    private FloatingActionButton buttonMap;
    private ImageButton logoutButton;
    private TextView appTitle;

    private final Handler animationHandler = new Handler(Looper.getMainLooper());
    private final int[] graffitiBackgrounds = {
            R.drawable.graffiti1, R.drawable.graffiti2,
            R.drawable.graffiti3, R.drawable.graffiti4
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Podłączamy widoki do zmiennych
        mainLayout = findViewById(R.id.mainLayout);
        appTitle = findViewById(R.id.appTitle);
        buttonCreate = findViewById(R.id.buttonCreate);
        buttonGallery = findViewById(R.id.buttonGallery);
        buttonMap = findViewById(R.id.buttonMap);
        logoutButton = findViewById(R.id.logoutButton);

        // Ustawiamy listenery (bez zmian)
        buttonCreate.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CameraActivity.class)));
        buttonGallery.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, GalleryActivity.class)));
        buttonMap.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, MapActivity.class)));

        logoutButton.setOnClickListener(v -> {
            SharedPreferences sharedPrefs = getSharedPreferences("StreetArtPrefs", Context.MODE_PRIVATE);
            sharedPrefs.edit().remove("USER_NICKNAME").apply();
            Intent intent = new Intent(MainActivity.this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ta funkcja jest kluczowa dla tła i animacji
        setupInitialScreen();
    }

    // PEŁNA, POPRAWNA WERSJA TEJ METODY
    private void setupInitialScreen() {
        Random rand = new Random();
        int bgId = graffitiBackgrounds[rand.nextInt(graffitiBackgrounds.length)];
        mainLayout.setBackgroundResource(bgId);

        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        mainLayout.startAnimation(fadeIn);

        Animation buttonAnim = AnimationUtils.loadAnimation(this, R.anim.button_enter);
        buttonCreate.startAnimation(buttonAnim);
        Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.button_bounce);
        buttonCreate.startAnimation(bounceAnim);

        String graffitiTitle = "StreetArt";
        int delay = 150;
        animateTitle(appTitle, graffitiTitle, delay);
    }

    // Pełna, poprawna wersja tej metody
    private void animateTitle(final TextView textView, final String text, final long delay) {
        animationHandler.removeCallbacksAndMessages(null);
        textView.setText("");
        final int[] index = {0};
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (textView.getParent() != null) {
                    if (index[0] < text.length()) {
                        textView.setText(text.substring(0, index[0] + 1));
                        index[0]++;
                        animationHandler.postDelayed(this, delay);
                    }
                }
            }
        }, delay);
    }
}