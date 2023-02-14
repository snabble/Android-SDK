@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble UI: The Android User Interface snabble SDK"

android {
    namespace = "io.snabble.sdk.ui"

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
        disable.addAll(listOf("LabelFor","MissingTranslation"))
    }
}

dependencies {
    val kotest_version = "5.4.0"

    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation (project(":utils"))
    api (project(":core"))
    api (project(":accessibility-toolbox"))

    implementation ("androidx.core:core-ktx:1.8.0")
    implementation ("androidx.appcompat:appcompat:1.4.1")
    implementation ("androidx.recyclerview:recyclerview:1.2.1")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("androidx.webkit:webkit:1.4.0")
    implementation ("androidx.viewpager2:viewpager2:1.0.0")
    implementation ("com.google.android.material:material:1.6.1")
    implementation ("androidx.gridlayout:gridlayout:1.0.0")
    implementation ("me.relex:circleindicator:2.1.6")
    implementation ("ch.datatrans:android-sdk:1.5.0")
    implementation ("com.google.android.gms:play-services-wallet:19.1.0")
    implementation ("eu.rekisoft.android.util:LazyWorker:2.1.0")
    implementation ("androidx.biometric:biometric:1.2.0-alpha04")
    implementation ("androidx.startup:startup-runtime:1.1.1")

    val camerax_version = "1.1.0-rc01"
    implementation ("androidx.camera:camera-core:${camerax_version}")
    implementation ("androidx.camera:camera-camera2:${camerax_version}")
    implementation ("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation ("androidx.camera:camera-view:${camerax_version}")
    implementation ("androidx.camera:camera-extensions:${camerax_version}")

    val navigation_version = "2.5.1"
    implementation ("androidx.navigation:navigation-runtime-ktx:$navigation_version")
    implementation ("androidx.navigation:navigation-fragment-ktx:$navigation_version")
    implementation ("androidx.navigation:navigation-ui-ktx:$navigation_version")

    //noinspection GradleDependency
    implementation ("commons-io:commons-io:2.5")
    //noinspection GradleDependency
    implementation ("com.google.zxing:core:3.5.1")
    implementation ("com.squareup.picasso:picasso:2.8")

    androidTestImplementation ("androidx.test:runner:1.4.0")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")

    implementation ("androidx.compose.material:material:1.2.1")
    implementation ("androidx.compose.material3:material3:1.0.0-rc01")
    implementation ("androidx.compose.ui:ui:1.2.1")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.2.1")
    implementation ("com.google.accompanist:accompanist-themeadapter-material3:0.28.0")

    debugImplementation ("androidx.compose.ui:ui-tooling:1.2.1")
    debugImplementation ("androidx.compose.ui:ui-test-manifest:1.2.1")

    // Compose previews won't work w/o this: https://issuetracker.google.com/issues/227767363
    debugImplementation ("androidx.customview:customview:1.2.0-alpha02")
    debugImplementation ("androidx.customview:customview-poolingcontainer:1.0.0")

    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.0-alpha02")

    testImplementation ("io.kotest:kotest-runner-junit5:$kotest_version")
    testImplementation ("io.kotest:kotest-assertions-core:$kotest_version")
    testImplementation ("io.mockk:mockk:1.13.3")
}

android.testOptions {
    unitTests.all {
        it.useJUnitPlatform()
    }
}
