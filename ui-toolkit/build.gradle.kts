@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version ("1.7.20")
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble UI-Toolkit: Additional views for simple and sample apps using the Snabble SDK"

android {
    namespace = "io.snabble.sdk.ui.toolkit"

    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 32
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            postprocessing {
                isRemoveUnusedCode = false
                isRemoveUnusedResources = false
                isObfuscate = false
                isOptimizeCode = false
                proguardFile("proguard-rules.pro")
            }
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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
        kotlinCompilerExtensionVersion = "1.4.1"
    }

    lint{
        disable.add("MissingTranslation")
    }
}

dependencies {
    val koin_version = "3.2.0"
    val kotest_version = "5.4.0"
    val nav_version = "2.5.2"

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    api("androidx.preference:preference-ktx:1.2.0")

    implementation (project (":accessibility-toolbox"))
    implementation (project (":utils"))
    implementation (project (":ui"))

    implementation ("androidx.activity:activity-compose:1.6.0")
    implementation ("androidx.appcompat:appcompat:1.5.1")
    implementation ("androidx.core:core-ktx:1.9.0")
    implementation ("androidx.compose.material:material:1.2.1")
    implementation ("androidx.compose.material3:material3:1.0.0-rc01")
    implementation ("androidx.compose.ui:ui:1.2.1")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.2.1")
    implementation ("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    implementation ("androidx.gridlayout:gridlayout:1.0.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.0-alpha02")
    implementation ("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation ("androidx.navigation:navigation-runtime-ktx:$nav_version")
    implementation ("androidx.startup:startup-runtime:1.1.1")

    implementation ("com.github.sebaslogen.resaca:resaca:2.3.0")

    implementation ("com.google.android.gms:play-services-maps:18.1.0")
    implementation ("com.google.android.material:material:1.6.1")
    implementation ("com.google.accompanist:accompanist-themeadapter-material3:0.28.0")

    implementation ("com.squareup.picasso:picasso:2.8")

    implementation ("commons-io:commons-io:2.11.0")

    implementation ("io.insert-koin:koin-core:$koin_version")
    implementation ("io.insert-koin:koin-android:$koin_version")
    implementation ("io.insert-koin:koin-android-compat:$koin_version")
    implementation ("io.insert-koin:koin-androidx-compose:$koin_version")

    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0")

    debugImplementation ("androidx.compose.ui:ui-tooling:1.2.1")
    debugImplementation ("androidx.compose.ui:ui-test-manifest:1.2.1")

    // Compose previews won't work w/o this: https://issuetracker.google.com/issues/227767363
    debugImplementation ("androidx.customview:customview:1.2.0-alpha02")
    debugImplementation ("androidx.customview:customview-poolingcontainer:1.0.0")

    testImplementation ("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation ("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation ("io.mockk:mockk:1.12.7")
}

android.testOptions {
    unitTests.all {
        it.useJUnitPlatform()
    }
}
