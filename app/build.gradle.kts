plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.mehran.farsiplex"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mehran.farsiplex"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    // The target device is a 32-bit ARM TV box: `-Pabi=armeabi-v7a` produces an
    // APK carrying only that ABI's native libraries. Without -Pabi the build
    // stays universal (arm64-v8a, armeabi-v7a, x86, x86_64).
    providers.gradleProperty("abi").orNull?.let { abi ->
        splits {
            abi {
                isEnable = true
                reset()
                include(abi)
                isUniversalApk = true
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// `-PfarsiplexLive` enables the network test that walks the whole
// catalog → stream chain against the live site.
tasks.withType<Test>().configureEach {
    systemProperty("farsiplex.live", providers.gradleProperty("farsiplexLive").isPresent.toString())
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    testImplementation("junit:junit:4.13.2")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Networking + HTML parsing (the site has no public catalog JSON API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Media playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
}
