# Snabble UI-Toolkit

Android UI-Toolkit for Snabble

## Onboarding

### Configuration

The configuration can be set up as JSON or any other type and needs to be deserializes into the
`OnboardingModel` class.

The following shows an example JSON config:

```json
 {
  "configuration": {
    "imageSource": "app-logo",
    "hasPageControl": true
  },
  "items": [
    {
      "imageSource": "onboarding_image",
      "title": "Onboarding.title",
      "text": "Onboarding.message",
      "footer": "Onboarding.caption",
      "nextButtonTitle": "Onboarding.next"
    },
    {
      "imageSource": "onboarding_image2",
      "text": "Enter a participating store, start shopping and hold the barcode of an article in front of the camera.",
      "nextButtonTitle": "Continue"
    },
    {
      "imageSource": "onboarding_image3",
      "text": "please accept the <a href='my.app://terms'>terms</a> of use and take note of the <a href='my.app://privacy'> policy </a>.",
      "link": "my.app://terms",
      "termsButtonTitle": "Show",
      "nextButtonTitle": "Accept"
    }
  ]
}
```

The following properties can be set:

#### Configuration

| Property    | Description | Values        |
| :---        |    :---   | :---          |
| imageSource    | Logo for the Retailer | Resource Identifier |
| hasPageControl     | if deactivated navigation is set to Button only | Boolean |

#### items

For each Item the following properties can be set:

| Property    | Description | Values        |
| :---        |    :---   | :---          |
| imageSource    | Image for the current Page | Resource Identifier, Url or String |
| title     | Title for the current Page | Resource Identifier or String|
| text    | Body text for the current Page | Resource Identifier or String|
| footer     | Footer text for the current Page | Resource Identifier or String|
| nextButtonTitle     | Name for the next button | Resource Identifier or String|
| prevButtonTitle     | Name for the previous button | Resource Identifier or String|
| termsButtonTitle     | Name for the terms button | Resource Identifier or String|
| link    | link to navigate to the terms | String |

Each field will be hidden if it's not set.

Addition:
1. The prevButtonTitle can only be used for back navigation.
2. If only one button is set, it will use the fullscreen.
3. If the name of the termsButtonTitle is set, the termsLink is needed.

### Usage

You can simply use the onboarding by setting the destination in your navigation components. Add the
argument model which is later used to pass the configuration for your onboarding.

```xml

<fragment 
    android:id="@+id/frag_onboarding"
    android:name="io.snabble.sdk.onboarding.OnboardingFragment" 
    android:label="OnboardingFragment"
    tools:layout="@layout/snabble_fragment_onboarding">
        <argument 
            android:name="model"
            app:argType="io.snabble.sdk.onboarding.entities.OnboardingModel" />
</fragment>
```

After that navigate to the onboarding fragment and pass the model as argument.

```Kotlin
     navController.navigate(R.id.frag_onboarding, bundleOf("model" to model))
```

### Terms and Privacy Custom Set up

Place the HTML file for the terms or privacy in the raw folder.

Set up the destination:

```xml
<fragment
    android:id="@+id/frag_tos"
    android:name="io.snabble.sdk.onboarding.terms.LegalFragment"
    android:label="terms of service">
        <argument
            android:name="resId"
            android:defaultValue="@raw/terms_agb"
            app:argType="reference"/>
        <argument
            android:name="imagePath"
            android:defaultValue="/android_asset/ic_terms_header.png"
            app:argType="string"/>
        <argument
            android:name="headerTitle"
            android:defaultValue="@string/Onboarding.Terms.introText"
            app:argType="reference"/>
        <deepLink
            android:id="@+id/deepLink"
            app:uri="my.app://terms" />
</fragment>
```

####Required: 
1. the arguments resId needs to be set as shown in the example, to display at least the Terms or privacy.
2. the deeplink need to be set to handle the given uri. The uri given in the configuration need to match the deeplink to navigate to the terms fragment.
optional: imagePath and headerTitle are optional arguments. If set they will be displayed as header on Top of the terms.

#### Onoarding Finished Event

To handle the onboarding finished event set up the OnboardingViewModel inside your activity or fragment

```Kotlin
     private val viewModel: OnboardingViewModel by viewModels()
```

Observe the onboardingSeen property to handle events than the onboarding is finished

```kotlin
    viewModel.onboardingSeen.observe(this) { 
        // Your code to handle finished event
        navController.popBackStack()
    }
```