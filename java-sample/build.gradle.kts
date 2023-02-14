@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "io.snabble.testapp"

    compileSdk = 33

    defaultConfig {
        applicationId = namespace
        minSdk = 21
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        var appId = "snabble-sdk-demo-app-oguh3x"
        var endpoint = "https://api.snabble.io"
        var secret = "2TKKEG5KXWY6DFOGTZKDUIBTNIRVCYKFZBY32FFRUUWIUAFEIBHQ===="

        val configFile = project.rootProject.file("local.properties")
        if (configFile.exists()) {
            val properties = Properties()
            properties.load(configFile.inputStream())

            appId = properties.getProperty("snabble.appId", appId)
            endpoint = properties.getProperty("snabble.endpoint", endpoint)
            secret = properties.getProperty("snabble.secret", secret)
        }

        manifestPlaceholders.putAll( mapOf("appId" to appId, "endpoint" to endpoint, "secret" to secret))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        freeCompilerArgs =  listOf(
            *kotlinOptions.freeCompilerArgs.toTypedArray(),
            "-Xjvm-default=all"
        )
        jvmTarget = "1.8"
    }
}

dependencies {
    coreLibraryDesugaring ("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation(project(":core"))
    implementation(project(":ui"))

    // in your app you need to use those dependencies:
    // implementation 'io.snabble.sdk:core:{currentVersion}'
    // implementation 'io.snabble.sdk:ui:{currentVersion}'

    implementation("androidx.appcompat:appcompat:1.4.1")

    //noinspection GradleDependency
    implementation ("commons-io:commons-io:2.5")
    implementation ("com.google.android.material:material:1.6.1")

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test:runner:1.4.0")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
}
