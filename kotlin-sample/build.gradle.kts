@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("io.snabble.setup") version("1.0.1")
}

android {
    namespace = "io.snabble.sdk.sample"

    compileSdk = 33

    defaultConfig {
        applicationId = namespace
        minSdk = 21
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        freeCompilerArgs = listOf(
            *kotlinOptions.freeCompilerArgs.toTypedArray(),
            "-Xjvm-default=all"
        )
        jvmTarget = "1.8"
    }


    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion  = "1.4.1"
    }
}

snabble {
    production {
        appId = "snabble-sdk-demo-app-oguh3x"
        secret = "2TKKEG5KXWY6DFOGTZKDUIBTNIRVCYKFZBY32FFRUUWIUAFEIBHQ===="
        prefetchMetaData = true
    }
    staging {
        appId = "snabble-sdk-demo-app-oguh3x"
        secret = "P3SZXAPPVAZA5JWYXVKFSGGBN4ZV7CKCWJPQDMXSUMNPZ5IPB6NQ===="
    }
    testing {
        appId = "snabble-sdk-demo-app-oguh3x"
        secret = "BWXJ2BFC2JRKRNW4QBASQCF2TTANPTVPOXQJM57JDIECZJQHZWOQ===="
    }
}

dependencies {
    val nav_version = "2.5.2"

    implementation("androidx.preference:preference-ktx:1.2.0")

    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation (project(":core"))
    implementation (project(":ui"))
    implementation (project(":ui-toolkit"))

    // in your app you need to use those dependencies:
    // implementation 'io.snabble.sdk:core:{currentVersion}'
    // implementation 'io.snabble.sdk:ui:{currentVersion}'

    implementation ("androidx.appcompat:appcompat:1.5.1")
    implementation ("androidx.activity:activity-compose:1.6.0")
    implementation ("androidx.compose.material:material:1.2.1")
    implementation ("androidx.compose.ui:ui:1.2.1")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.2.1")
    implementation ("androidx.compose.ui:ui-util:1.2.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation ("androidx.core:core-ktx:1.9.0")
    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation ("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation ("androidx.navigation:navigation-ui-ktx:$nav_version")

    implementation ("com.google.android.material:material:1.6.1")

    implementation ("com.jakewharton:process-phoenix:2.1.2")

    implementation ("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")

    debugImplementation ("androidx.compose.ui:ui-tooling:1.2.1")
    debugImplementation ("androidx.compose.ui:ui-test-manifest:1.2.1")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")

}
