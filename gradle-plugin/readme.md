# Snabble Gradle Plugin [![License: MIT,][license-img]][license-url] [![at Gradle Plugin Portal,][gradle-img]][gradle-url]

The snabble Gradle Plugin is for the simplest setup of the snabble Android SDK. With this plugin you can reduce the
setup to 3 lines in your `build.gradle`. You can also download the manifest in your CI to bundle the latest metadata
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
snabble.appId = 'your-app-id'
snabble.secret = 'your-app-secret'
```

That's it. However we recommend to use the normal dsl syntax like this:

```groovy
snabble {
  appId = '...'
  secret = '...'
  prefetchMetaData = true
}
```

With the last line added the manifest will be downloaded for each release build. If you want to use it also in debug
builds you need to execute the gradle task `downloadSnabbleMetadata` or set `prefetchMetaDataForDebugBuilds = true`.
Caching is applied so the file won't be downloaded on each build (except clean builds).

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