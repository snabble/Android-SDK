# Snabble Gradle Plugin [![License: MIT,][license-img]][license-url] [![at Gradle Plugin Portal,][gradle-img]][gradle-url]

The snabble Gradle Plugin is for the simplest setup of the snabble Android SDK. With this plugin you can reduce the
SDK integration to 4 lines in your codebase. You can also download the manifest in your CI to bundle the latest metadata
with your app so that your user it can use your app with all required meta data.

## Usage

### Add plugin
Using the [plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

```groovy
plugins {
  id 'com.android.application'
  id 'io.snabble.setup' version '0.1.0' // add this line
}
```

### Using the plugin

The absolut minimum is to add those two lines to make the SDK already working:
```groovy
snabble.production.appId = 'your-app-id'
snabble.production.secret = 'your-app-secret'
```

To start the combined scanner you just need to start our activity:

```kotlin
startActivity(Intent(context, CombinedScannerActivity::class.java))
```

That's it. This is the absolut minimum integration.

However we recommend to use the normal DSL syntax to define your config like this:

```groovy
snabble.production {
  appId = 'your-app-id'
  secret = 'your-app-secret'
  prefetchMetaData = true
}
```

With the last line added the manifest will be downloaded for each release build. If you want to use it also in debug
builds you need to execute the gradle task `downloadSnabbleMetadata`.  
Caching is applied so the file won't be downloaded on each build (except clean builds).

When your app supports multiple stages in a single app (e.g. with a developer setting) then you can define multiple
secrets in your `build.gradle` like this:

```groovy
snabble {
  production {
    appId = 'your-app-id'
    secret = 'your-production-secret'
    prefetchMetaData = true
  }
  staging {
    appId = 'your-app-id'
    secret = 'your-staging-secret'
  }
  testing {
    appId = 'your-app-id'
    secret = 'your-testing-secret'
  }
}
```

To change the environment to staging you need to update this variable:

```kotlin
Snabble.userPreferences.environment = Environment.STAGING
```

Then you also need to restart the app. We use in our app for that the
[ProcessPhoenix](https://github.com/JakeWharton/ProcessPhoenix) library like this:

```kotlin
ProcessPhoenix.triggerRebirth(context)
```

For a seamless integration check out sample apps there you can see how you integrate our SDK it with out sample Apps.
There we show you how to customize our UI.

# Plugin development

If you want to contribute to this plugin you need:

1. Checkout this repository
2. Deploy the plugin to your local maven repository:

    ```shell
    ./gradlew publishToMavenLocal
    ```
3. Test your changes
4. Create a Pull Request

# Author

snabble GmbH, Bonn

[license-img]: https://img.shields.io/github/license/snabble/Android-SDK
[license-url]: ../LICENSE
[gradle-img]: https://img.shields.io/gradle-plugin-portal/v/io.snabble.setup
[gradle-url]: https://plugins.gradle.org/plugin/io.snabble.setup