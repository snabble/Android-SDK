# Sample apps

We have two sample apps one legacy app written in Java and a newer written in Kotlin. You can find
both apps in the git repository in the directories [java-sample] and [kotlin-sample].

## Requirements

Before you can start you need an app secrets which you can get via our sales team. You can find the
contact details on our [Homepage](https://snabble.io/en/contact).

The app secrets can be configures in your `local.properties` file (in the project root directory),
the sample app's `build.gradle` file or with code.

We recommend using the `local.properties` file. You cannot leak this way your secret accidentally,
since this file is by default in your `.gitignore`. This way you can also configure both sample apps
at once. In that file you need to add these three lines:

```
snabble.appId=<your-app-id>
snabble.endpoint=<your-endpoint>
snabble.secret=<your-secret>
```

If you want to add it to your `build.gradle` file search for the comment ending with
`Snabble secrets` and put your secrets there.

The third way is to setup the config via code:

<!--codeinclude-->
[Kotlin](../../kotlin-sample/src/main/java/io/snabble/sdk/sample/LoadingActivity.kt) inside_block:config
[Java](../../java-sample/src/main/java/io/snabble/testapp/App.java) inside_block:config
<!--/codeinclude-->

 [java-sample]: https://github.com/snabble/Android-SDK/tree/master/java-sample
 [kotlin-sample]: https://github.com/snabble/Android-SDK/tree/master/kotlin-sample