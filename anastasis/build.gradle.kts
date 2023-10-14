import java.io.ByteArrayOutputStream

plugins {
    kotlin("kapt")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
}

val qtartVersion = "0.9.3-dev.15"
val composeVersion = "1.5.3"
val minifyDebug by extra(false)

fun versionCodeEpoch() = (System.currentTimeMillis() / 1000).toInt()
fun gitCommit(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "--short=7", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

android {
    namespace = "net.taler.anastasis"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.taler.anastasis"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = minifyDebug
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    flavorDimensions += "distributionChannel"
    productFlavors {
        create("fdroid") {
            dimension = "distributionChannel"
            applicationIdSuffix = ".fdroid"
        }
        create("google") {
            dimension = "distributionChannel"
        }
        create("nightly") {
            dimension = "distributionChannel"
            applicationIdSuffix = ".nightly"
            versionCode = versionCodeEpoch()
            versionNameSuffix = " (${gitCommit()})"
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
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }
    packaging {
        jniLibs {
            keepDebugSymbols += arrayOf("**/*.so")
        }
        resources {
            excludes += arrayOf("META-INF/{AL2.0,LGPL2.1}/*.kotlin_module")
        }
    }
}

dependencies {
    implementation(project(":taler-kotlin-android"))

    // Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // Compose
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation(platform("androidx.compose:compose-bom:2022.10.00"))
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-graphics:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.navigation:navigation-compose:2.7.4")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")

    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    kapt("com.google.dagger:hilt-android-compiler:2.44")

    // JNI/JNA
    implementation("net.java.dev.jna:jna:5.13.0@aar")

    // Kotlin/KotlinX
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // Taler
    implementation("net.taler:qtart:$qtartVersion@aar")
}

kapt {
    correctErrorTypes = true
}