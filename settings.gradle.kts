pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Ta linia dodaje brakujący adres do biblioteki 'colorpicker'
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "StreetArtV2"
include(":app")