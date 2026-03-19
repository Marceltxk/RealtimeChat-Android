plugins {

// ou o mais recente compatível
    id("org.jetbrains.kotlin.android") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.compose" )version "2.0.0" // se usar Compose
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ksp)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.isacsilva.ufumessenger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.isacsilva.ufumessenger"
        minSdk = 30
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-functions-ktx:21.2.1")
    implementation(libs.accompanist.permissions)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.playintegrity.debug)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.coil.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.foundation)
    kapt(libs.hilt.compiler)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation(libs.androidx.core.splashscreen)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation("androidx.media3:media3-exoplayer:1.8.0") // Verifique a versão mais recente
    implementation("androidx.media3:media3-ui:1.8.0")        // Para componentes de UI do player
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

tasks.register<Wrapper>("wrapper") {
    gradleVersion = "8.4" // Você pode usar uma versão recente e estável do Gradle
    distributionType = Wrapper.DistributionType.ALL
}