@file:Suppress("UnstableApiUsage")

import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(libs.plugins.androidApplication.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
}

android {
    namespace = "io.snabble.testapp"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = namespace
        minSdk = libs.versions.minSdk.get().toInt()
        @Suppress("Deprecation")
        targetSdk = libs.versions.targetSdk.get().toInt()
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
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(project(":core"))
    implementation(project(":ui"))

    // in your app you need to use those dependencies:
    // implementation 'io.snabble.sdk:core:{currentVersion}'
    // implementation 'io.snabble.sdk:ui:{currentVersion}'

    implementation(libs.androidx.appcompat)

    //noinspection GradleDependency
    implementation (libs.android.material)
    implementation (libs.commonsIo)

    testImplementation (libs.junit)
    androidTestImplementation (libs.test.espressoCore)
    androidTestImplementation (libs.test.runner)
}
