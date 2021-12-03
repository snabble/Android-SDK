# Installation

## Requirements

minSdkVersion = 21  
compileSdkVersion = 29  
java 8

androidx and a material 3 theme for ui components

### Using the snabble GitHub Repository

Add the snabble maven repository for out and the Datatrans SDK:

``` groovy
repositories {
    maven {
        url 'https://raw.githubusercontent.com/snabble/maven-repository/releases'
    }
}
```

Then add the library to your dependencies.

``` groovy
dependencies {
    // core library
    implementation 'io.snabble.sdk:core:{{ extra.sdk_version }}'
    
    // user interface library
    implementation 'io.snabble.sdk:ui:{{ extra.sdk_version }}'
}
```

### Locally

The library can be installed to the local maven repository using:

``` sh
./gradlew publishToMavenLocal
```

Make sure you add maven local to your repositories in your gradle script.

``` groovy
repositories {
    mavenLocal()
}
```

Then add the library to your dependencies. (Note: The + means it always uses the latest version)

``` groovy
dependencies {
    implementation 'io.snabble.sdk:core:{{ extra.sdk_version }}'
    implementation 'io.snabble.sdk:ui:{{ extra.sdk_version }}'
    implementation 'io.snabble.sdk:ui-integration:{{ extra.sdk_version }}'
}
```

## Usage
``` java
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
        SnabbleUI.useProject(project);

        // select the first shop for demo purposes, ideally this should be done with
        // geofencing or a manual user selection
        if (project.getShops().length > 0) {
            project.getCheckout().setShop(project.getShops()[0]);
        }

        // optional: set a loyalty card id for identification, for demo purposes
        // we invent one here
        project.setLoyaltyCardId("testAppUserLoyaltyCardId");
        
        // optional: load a bundled database file from the assets folder
        // this lowers the download size of database updates and the database is immediatly
        // available offline
        project.getProductDatabase().loadDatabaseBundle("db.sqlite3", revision, major, minor);
        
        // recommended: download the latest product database for offline availability
        // it is highly recommended to call this in shorter time frames than config.maxProductDatabaseAge is set at
        // since the local database is only used if the time since the last update is smaller than 
        // config.maxProductDatabaseAge, which defaults to 1 hour
        //
        // a good place for this is the onStart() method of your activity
        // database updates are usually very small since we are using delta updates for updating the database
        //
        // also a good place for database updates are background schedulers like 
        // https://developer.android.com/topic/libraries/architecture/workmanager
        project.getProductDatabase().update();
    }

    @Override
    public void onError(Snabble.Error error) {
        // connecton error if no metadata file is bundled or config error
        // if no appId or secret is provided
    }
});
```