@file:Suppress("UnstableApiUsage")

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble UI: The Android User Interface snabble SDK"

android {
    namespace = "io.snabble.sdk.ui"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        @Suppress("Deprecation")
        targetSdk = libs.versions.targetSdk.get().toInt()
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
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    lint{
        disable.addAll(listOf("LabelFor","MissingTranslation"))
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation (project(":utils"))
    api (project(":core"))
    api (project(":accessibility-toolbox"))

    implementation(libs.airbnb.lottie)
    implementation(libs.androidx.appcompat)
    implementation (libs.androidx.biometric)
    implementation (libs.androidx.cardview)
    implementation (libs.androidx.core.ktx)
    implementation (libs.androidx.gridlayout)
    implementation (libs.android.material)
    implementation (libs.androidx.recyclerview)
    implementation (libs.androidx.startupRuntime)
    implementation (libs.androidx.swiperefreshlayout)
    implementation (libs.androidx.viewpager2)
    implementation (libs.androidx.webkit)
    implementation (libs.commonsIo)
    implementation (libs.datatrans.androidSdk)
    implementation (libs.google.zxing.core)
    implementation (libs.googlePlayServices.wallet)
    implementation (libs.picasso)
    implementation (libs.rekisoftLazyWorker)
    implementation (libs.relex.circleindicator)

    implementation(libs.bundles.camera)
    implementation(libs.bundles.navigation)

    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    testImplementation (libs.kotest.assertionsCore)
    testImplementation (libs.kotest.runnerJunit)
    testImplementation (libs.mock)
    androidTestImplementation (libs.test.espressoCore)
    androidTestImplementation (libs.test.runner)
}

android.testOptions {
    unitTests.all {
        it.useJUnitPlatform()
    }
}
