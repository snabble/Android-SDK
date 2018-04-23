# Snabble

Android SDK for Snabble

## Installation

#### Using the snabble github Repository

Add the snabble Repository to your gradle Repositories

```
repositories {
    maven {
        url 'https://raw.githubusercontent.com/snabble/maven-repository/releases'
    }
}
```

Then add the library to your dependencies. 

```gradle
dependencies {
    // core library
    implementation 'io.snabble.sdk:core:{currentVersion}'
    
    // user interface library
    implementation 'io.snabble.sdk:ui:{currentVersion}'
    
    // user interface integration library, entirely optional,
    // for more seamless and easier integration in apps
    implementation 'io.snabble.sdk:ui-integration:{currentVersion}'
}
```

#### Locally

The library can be installed to the local maven repository using:

```sh
./gradlew install 
```

Make sure you add maven local to your repositories in your gradle script.

```gradle
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
```
//you may enable debug logging to see requests made by the sdk, and other various logs
SnabbleSdk.setDebugLoggingEnabled(true);

SnabbleSdk.Config config = new SnabbleSdk.Config();
config.metadataUrl = "/api/{projectId}/metadata/app/android/android/{appVersion}";
config.endpointBaseUrl = "api.snabble.io";
config.clientToken = clientToken;
config.projectId = "demo";
config.bundledMetadataAssetPath = "metadata.json";
config.productDbName = "products.sqlite3";
config.productDbBundledAssetPath = "products.sqlite3";
config.productDbBundledRevisionId = getBundledRevisionId();
config.productDbBundledSchemaVersionMajor = getBundledMajor();
config.productDbBundledSchemaVersionMinor = getBundledMinor();

SnabbleSdk.setup(this, config, new SnabbleSdk.SetupCompletionListener() {
    @Override
    public void onReady(SnabbleSdk sdk) {
        //registers this sdk instance globally for use with ui components
        SnabbleUI.registerSdkInstance(sdk);
    }

    @Override
    public void onError(final SnabbleSdk.Error error) {
        //various errors, like no space left on device
        //network connections could not be made (if no bundled data is provided)
        //see the enum declaration for more info
    }
});
```

## Author

snabble GmbH, Bonn
