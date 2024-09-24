@file:Suppress("UnstableApiUsage")

plugins {
    id(libs.plugins.androidLibrary.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
}

apply {
    from("../scripts/maven.gradle")
}

description = "Accessibility-Toolbox: Make it easy to make your app accessible"

android {
    namespace = "io.snabble.accessibility"

    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        targetSdk = libs.versions.targetSdk.get().toInt()
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }

    kotlinOptions {
        freeCompilerArgs = listOf(
            *kotlinOptions.freeCompilerArgs.toTypedArray(),
            "-Xjvm-default=all"
        )
        jvmTarget = "17"
    }

    lint {
        disable.addAll(listOf("LabelFor", "MissingTranslation"))
    }
}

dependencies {
    implementation(project(":utils"))

    implementation(libs.androidx.core.ktx)
}
