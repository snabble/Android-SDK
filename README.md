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
}
```

## Usage

You can initialize the SDK by specifying metadata in the AndroidManifest.xml

```
<meta-data
android:name="snabble_app_id"
android:value="YOUR_APP_ID" />

<meta-data
android:name="snabble_secret"
android:value="YOUR_SECRET" />
```

You also can initialize the SDK programmatically by specifying in the AndroidManifest.xml: 

```
<meta-data
android:name="snabble_auto_initialization_disabled"
android:value="true" />
```

And then initialize the SDK via code.

```kotlin
val config = Config(
    appId = YOUR_APP_ID,
    secret = YOUR_SECRET,
)
Snabble.setup(application, config)
```

To observe the current initialization state of the SDK use: 

```kotlin
Snabble.initializationState.observe(this) {
    when(it) {
        InitializationState.INITIALIZING -> {}
        InitializationState.INITIALIZED -> {
            val projects = Snabble.projects // access to projects
        }
        InitializationState.ERROR -> {
            // error detailing why the initialization failed
            val error = Snabble.error 
            // you can call setup again without arguments to retry the initialization 
            // e.g. on network failure
            Snabble.setup() 
        }
    }
}
```

## Light mode themes

If using a theme that is explicitly only light mode (and not a DayNight theme) you need to set

```kotlin
AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
```

or else some resources may get grabbed from the "-night" folders when the device is set to night mode

## Author

snabble GmbH, Bonn
