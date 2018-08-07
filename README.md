[![Build Status](https://travis-ci.org/snabble/Android-SDK.svg?branch=master)](https://travis-ci.org/snabble/Android-SDK)

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
        //you may enable debug logging to see requests made by the sdk, and other various logs
        Snabble.setDebugLoggingEnabled(true);

        Snabble.Config config = new Snabble.Config();
        config.appId = <your app id>
        config.secret = <your secret>
        
        // optional: provide a metadata file, store in the assets. That allows the sdk 
        // init without requiring a network connection.
        config.bundledMetadataAssetPath = "metadata.json";

        final Snabble snabble = Snabble.getInstance();
        snabble.setup(this, config, new Snabble.SetupCompletionListener() {
            @Override
            public void onReady() {
                // get the first project, there can be multiple projects per app
                project = snabble.getProjects().get(0);

                // registers this project globally for use with ui components
                SnabbleUI.registerProject(project);

                // select the first shop for demo purposes
                if (project.getShops().length > 0) {
                    project.getCheckout().setShop(project.getShops()[0]);
                }

                // optional: download the latest product database for offline availability
                project.getProductDatabase().update();

                // optional: set a loyalty card id for identification, for demo purposes
                // we invent one here
                project.setLoyaltyCardId("testAppUserLoyaltyCardId");
                
                // optional: load a bundled database file from the assets folder
                // this lowers the download size of database updates and the database is immediatly
                // available offline
                project.getProductDatabase().loadDatabaseBundle("db.sqlite3", revision, major, minor);

                callback.done();
            }

            @Override
            public void onError(Snabble.Error error) {
                callback.error("SdkError: " + error.toString());
            }
        });
```

## Author

snabble GmbH, Bonn
