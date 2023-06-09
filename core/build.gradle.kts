@file:Suppress("UnstableApiUsage")

@Suppress("DSL_SCOPE_VIOLATION") plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    alias(libs.plugins.kotlin.serialization)
}

apply {
    from("../scripts/maven.gradle")
}

description = "Snabble Core: The business logic of the Snabble SDK"

android {
    namespace = "io.snabble.sdk"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        @Suppress("Deprecation")
        targetSdk = libs.versions.targetSdk.get().toInt()
        consumerProguardFile("proguard-rules.pro")
        buildConfigField("String", "VERSION_NAME", "\"${project.extra.get("sdkVersion")}\"")

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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(project(":utils"))

    //noinspection GradleDependency
    implementation(libs.apache.commonsLang3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.caverock.androidsvgAar)
    implementation(libs.commonsIo)
    implementation(libs.iban4j)
    implementation(libs.googlePlayServices.wallet)
    api(libs.gson)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.rekisoftLazyWorker)

    api(libs.squareup.okhttp3.okhttp)
    implementation(libs.squareup.okhttp3.loggingInterceptor)

    testImplementation(libs.junit)
    testImplementation(libs.roboletric)
    testImplementation(libs.squareup.okhttp3.mockwebserver)
    testImplementation(libs.koltin.reflect)
    testImplementation(libs.androidx.coreTesting)
}
