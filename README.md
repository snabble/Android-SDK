![Build](https://github.com/snabble/Android-SDK/workflows/Build/badge.svg)

# Snabble

Android SDK for Snabble

## Requirements

```
minSdkVersion = 21
compileSdkVersion = 29
java 8

androidx and a material 3 theme for ui components
```

## Installation

#### Using the snabble github Repository

Add the snabble and Datatrans Repository to your gradle Repositories

```groovy
repositories {
    maven {
        url 'https://raw.githubusercontent.com/snabble/maven-repository/releases'
    }
}
```

Then add the library to your dependencies. 

```groovy
dependencies {
    // core library
    implementation 'io.snabble.sdk:core:{currentVersion}'
    
    // user interface library
    implementation 'io.snabble.sdk:ui:{currentVersion}'
}
```

#### Locally

The library can be installed to the local maven repository using:

```shell
./gradlew publishToMavenLocal
```

Make sure you add maven local to your repositories in your gradle script.

```groovy
repositories {
    mavenLocal()
}
```

Then add the library to your dependencies. (Note: The + means it always uses the latest version)

```gradle
dependencies {
    implementation 'io.snabble.sdk:core:+'
    implementation 'io.snabble.sdk:ui:+'
    implementation 'io.snabble.sdk:ui-integration:+'
}
```

## Usage
```kotlin
val config = Config(
    appId = YOUR_APP_ID,
    secret = YOUR_SECRET,
)

// you may enable debug logging
Snabble.setDebugLoggingEnabled(BuildConfig.DEBUG)

Snabble.setup(application, config, object : Snabble.SetupCompletionListener {
    override fun onReady() {
        // an application can have multiple projects, for example for
        // multiple independent regions / countries
        val project = Snabble.projects.first()

        // check in to the first shop - you can use CheckInManager if you want
        // to use geofencing
        Snabble.checkedInShop = project.shops.first()
    }
})
```

## Light mode themes

If using a theme that is explicitly only light mode (and not a DayNight theme) you need to set

```
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
```

or else some resources may get grabbed from the "-night" folders when the device is set to night mode

## Author

snabble GmbH, Bonn
