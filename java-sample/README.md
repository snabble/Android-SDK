# Java Sample App

This is our sample App to show you how to integrate our SDK in a Java app using classic Activity
navigation. We have also a newer Kotlin App using modern Navigation components which you can find in
the [kotlin-sample](../kotlin-sample) directory.

## Requirements

Before you can start you need an app secrets which you can get via our sales team. You can find the
contact details on our [Homepage](https://snabble.io/en/contact).

The app secrets can be configures in your `local.properties` file (in the project root directory),
the sample app's `build.gradle` file or directly in the `Config` object in `init` function of the
[`App`](src/main/java/io/snabble/testapp/App.java).

In the `local.properties` file you need to add these three lines:

```
snabble.appId=<your-app-id>
snabble.endpoint=<your-endpoint>
snabble.secret=<your-secret>
```

If you want to add it to your `build.gradle` file search for the comment ending with
`Snabble secrets` and put your secrets there.