plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    alias(libs.plugins.google.gms.google.services) // Plugin para o KAPT (Kotlin Annotation Processing Tool)
}

android {
    namespace = "com.cmu.a75hard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cmu.a75hard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core e Jetpack Compose
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.ui:ui:1.1.0")
    implementation("androidx.compose.ui:ui-graphics:1.1.0")
    implementation ("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.1.0")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.navigation:navigation-compose:2.4.0")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.material:material:1.5.0")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.5.0")
    implementation ("androidx.navigation:navigation-compose:2.6.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation ("androidx.compose.material:material:1.5.0")
    implementation ("androidx.compose.material:material-icons-core:1.5.0")
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.foundation.android)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)


    // Dependências de testes
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.1.0")

    // Dependências para depuração
    debugImplementation("androidx.compose.ui:ui-tooling:1.1.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.1.0")

    // Google Maps e localização
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.maps.android:maps-compose:6.2.1")
    implementation("com.google.maps.android:android-maps-utils:3.9.0")
    implementation ("com.google.accompanist:accompanist-permissions:0.31.1-alpha")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Retrofit e Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.8.8")

    // Coil para carregar imagens
    implementation("io.coil-kt:coil-compose:2.1.0")

    // Ktor
    implementation("io.ktor:ktor-client-core:2.0.0")
    implementation("io.ktor:ktor-client-cio:2.0.0")
    implementation("io.ktor:ktor-client-serialization:2.0.0")
    implementation("io.ktor:ktor-client-json:2.0.0")
    implementation ("androidx.work:work-runtime-ktx:2.7.1")

    // Room para persistência de dados
    val roomVersion = "2.6.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    // Encriptacao de Password
    implementation ("at.favre.lib:bcrypt:0.9.0")

    // Player de videos
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Gestao de sessao (EncrypedSharedPreferences)
    implementation ("androidx.security:security-crypto:1.1.0-alpha03")

    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.compose.foundation:foundation:1.5.0")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation("androidx.compose.material3:material3:1.0.1")
    implementation("androidx.compose.material:material:1.4.3")


    // Retrofit (TheMealDB)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")


    implementation("androidx.camera:camera-core:1.3.0-alpha06")
    implementation("androidx.camera:camera-camera2:1.3.0-alpha06")
    implementation("androidx.camera:camera-lifecycle:1.3.0-alpha06")
    implementation("androidx.camera:camera-view:1.3.0-alpha06")

    implementation("com.google.guava:guava:31.1-android")



}
