@file:Suppress("UnstableApiUsage")

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(libs.plugins.androidApplication.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.snabbleSetup)
}

android {
    namespace = "io.snabble.sdk.sample"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = namespace
        minSdk = libs.versions.minSdk.get().toInt()
        @Suppress("Deprecation")
        targetSdk = libs.versions.targetSdk.get().toInt()
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
        kotlinCompilerExtensionVersion = "1.4.1"
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
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":ui-toolkit"))

    // in your app you need to use those dependencies:
    // implementation 'io.snabble.sdk:core:{currentVersion}'
    // implementation 'io.snabble.sdk:ui:{currentVersion}'

    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.constraintlayoutCompose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycleExtension)
    implementation(libs.androidx.lifecycleLiveData)
    implementation(libs.android.material)
    implementation(libs.androidx.navigation.fragmentKtx)
    implementation(libs.androidx.navigation.uiKtx)
    implementation(libs.androidx.preferences)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.uiUtil)
    implementation(libs.jakewhartonProcessPhoenix)
    implementation(libs.kotlin.stdlib)

    debugImplementation(libs.compose.uiTestManifest)
    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.test.espressoCore)
    androidTestImplementation(libs.test.ext.junit)

}
