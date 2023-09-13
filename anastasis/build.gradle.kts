import java.io.ByteArrayOutputStream

plugins {
    kotlin("kapt")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
}

val qtartVersion = "0.9.3-dev.15"
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

@Suppress("UnstableApiUsage")
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
        kotlinCompilerExtensionVersion = "1.4.6"
    }
    packagingOptions {
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
    implementation("net.taler:qtart:$qtartVersion@aar")
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation(platform("androidx.compose:compose-bom:2022.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("com.google.dagger:hilt-android:2.44")
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
    implementation("io.matthewnelson.encoding:base32:2.0.0")
    kapt("com.google.dagger:hilt-android-compiler:2.44")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}