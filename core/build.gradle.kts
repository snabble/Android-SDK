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
description = "Snabble Core: The business logic of the snabble SDK"

android {
    namespace = "io.snabble.sdk"

    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 32
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    implementation(project(":utils"))

    //noinspection GradleDependency
    implementation("commons-io:commons-io:2.5")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.iban4j:iban4j:3.2.1")
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("com.google.android.gms:play-services-wallet:19.1.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.biometric:biometric:1.2.0-alpha04")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")
    implementation("androidx.lifecycle:lifecycle-common:2.5.1")

    api("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("eu.rekisoft.android.util:LazyWorker:2.1.0")

    api("com.google.code.gson:gson:2.9.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
    testImplementation("androidx.arch.core:core-testing:2.1.0")
}
