# The gradle plugin for Snabble [![License: MIT,][license-img]][license-url] [![at Gradle Plugin Portal,][gradle-img]][gradle-url]

TODO add background why to use this plugin

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

TODO

```groovy
snabble {
  appId = '...'
  secret = '...'
  prefetchMetaData = true
}
```

# Development

1. You need to checkout this repository
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