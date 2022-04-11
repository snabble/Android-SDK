# Fancy Sample App

This is a advanced sample App to show how to customize the SDK appearance to make the integration as
smooth as possible to your own design. It also shows how the app would look like with Material You
enabled. Please also check the other two sample apps in [kotlin-sample](../kotlin-sample) and
[java-sample](../java-sample) directory.

## Important notes

This is a tech demo what you can currently do with the Android SDK. That does not mean that the same
features are currently implemented in the iOS SDK too. The custom dialog confirmation dialog is a
technical preview and not yet supported on iOS.

## Requirements

Before you can start you need an app secrets which you can get via our sales team. You can find the
contact details on our [Homepage](https://snabble.io/en/contact).

The app secrets can be configures in your `local.properties` file (in the project root directory),
the sample app's `build.gradle` file or directly in the `Config` object in `initSdk` function of
[`LoadingActivity`](src/main/java/io/snabble/sdk/customization/LoadingActivity.kt).

In the `local.properties` file you need to add these three lines:

```
snabble.appId=<your-app-id>
snabble.endpoint=<your-endpoint>
snabble.secret=<your-secret>
```

If you want to add it to your `build.gradle` file search for the comment ending with
`Snabble secrets` and put your secrets there.