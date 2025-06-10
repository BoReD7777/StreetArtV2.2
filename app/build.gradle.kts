plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") // <-- DODAJ TĘ LINIĘ


}

android {
    // Upewnij się, że namespace pasuje do Twojego projektu
    namespace = "com.example.streetartv2"
    compileSdk = 35

    defaultConfig {
        // Upewnij się, że applicationId pasuje do Twojego projektu
        applicationId = "com.example.streetartv2"
        minSdk = 24
        //noinspection EditedTargetSdkVersion
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Core Android & Jetpack
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("io.ktor:ktor-client-android:2.3.12")
    // Google Play Services
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Biblioteki zewnętrzne
    implementation("io.coil-kt:coil:2.6.0") // Do ładowania obrazków
    implementation("com.github.QuadFlask:colorpicker:0.0.13") // Do palety kolorów

    // Supabase (tylko potrzebne moduły)
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.5.2") // Do bazy danych
    implementation("io.github.jan-tennert.supabase:storage-kt:2.5.2") // Do przechowywania plików

    // Serializacja
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Zależności testowe
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}