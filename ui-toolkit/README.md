# Snabble UI-Toolkit

Android UI-Toolkit for Snabble

####Table of Contents
[Onboarding](#Onboarding)
[Shopfinder](#Shopfinder)

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

##Shopfinder

### Configuration

For the shop finder and the details page the location tracking needs to be started as soon as permission is granted.
You can either start location tracking directly via an instance of the location manager or with the snabble check in manager if used.
```kotlin
    LocationManager.getInstance(this).startTrackingLocation()
    Snabble.checkInManager.startUpdating()
```

You can stop the tracking over the same stop method.

### Set up

To set up the shop finder:

1. Navigate to the ShopList fragment
1.1 Set it up in ur Navigation file

```xml
     <fragment
        android:id="@+id/navigation_shops"
        android:name="io.snabble.sdk.shopfinder.ShopListFragment"
        android:label="your label" />
```
1.2 Navigate to your destination
```kotlin
    navController.navigate(R.id.navigation_shops)
```
2. Set it up as part of a Navigation Bar
2.1 In your menu file set up the destination for the tab
```xml
    <item
        android:id="@id/navigation_shops"
        android:icon="your icon"
        android:title="your title" />
```
3.Execute the SnabbleUi-Toolkit event to start the shop finder

```kotlin
    SnabbleUiToolkit.executeAction(context,SnabbleUiToolkit.Event.SHOW_SHOP_LIST)
```

4.Extend the 'ShopListFragment' to implement custom behaviour (e.g back button for toolbar etc.)

The details page for each shop opens by default. If further customizations need to be done you can extend 
the 'ShopDetailsFragment' and navigate to the new destination by overwriting the SnabbleUi-Toolkit event 
SHOW_SHOP_LIST_DETAILS

```kotlin
    SnabbleUiToolkit.setUiAction(
        this@MainActivity,
        SnabbleUiToolkit.Event.SHOW_DETAILS_SHOP_LIST) 
        {_, args -> navigate(R.id.navigation_shops_details, args)}
```


###Customization

You can set up a Toolbar title by overwriting the following strings

```xml
<resources>
    <string name="Snabble.Shop.Finder.title" />
    <string name="Snabble.Shop.Details.title" />
</resources>
```

By default the shop list title is set to "shops" and the details page takes the store name as title.

#### Optional

You can set up button which only appears after the check in. 
To set up the button overwrite the following string
```xml
<resources>
    <string name="Snabble.Shop.Details.button" />
</resources>
```

to set up an event for the button click set up an ui action for the SHOW_DETAILS_BUTTON_ACTION event

```kotlin
    SnabbleUiToolkit.setUiAction(
        this@MainActivity,
        SnabbleUiToolkit.Event.SHOW_DETAILS_BUTTON_ACTION)
        { _, _ -> /**your action*/}
```