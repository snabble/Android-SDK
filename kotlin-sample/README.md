# Kotlin Sample App

This is our sample App to show you how to integrate our SDK in a Kotlin app using modern Navigation
components. If you use classic Activity navigation, you can check out our Java Sample App in the
[java-sample](../java-sample) directory.

## Requirements

Before you can start you need an app secrets which you can get via our sales team. You can find the
contact details on our [Homepage](https://snabble.io/en/contact).

The app secrets can be configures in your `local.properties` file (in the project root directory),
the sample app's `build.gradle` file or directly in the `Config` object in `initSdk` function of
[`LoadingActivity`](src/main/java/io/snabble/sdk/sample/LoadingActivity.kt).

In the `local.properties` file you need to add these three lines:

```
snabble.appId=<your-app-id>
snabble.endpoint=<your-endpoint>
snabble.secret=<your-secret>
```

If you want to add it to your `build.gradle` file search for the comment ending with
`Snabble secrets` and put your secrets there.