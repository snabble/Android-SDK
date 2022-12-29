# Snabble UI-Toolkit

Android UI-Toolkit for Snabble

## Table of Contents

1. [Onboarding](#Onboarding)
2. [Shopfinder](#Shopfinder)
3. [DynamicView](#DynamicView)

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

| Property       | Description                                                | Values              |
| :---           | :---                                                       | :---                |
| imageSource    | Logo for the Retailer                                      | Resource Identifier |
| hasPageControl | Deactivated swipe navigation and enforce Button navigation | Boolean             |

#### Items

For each item respectively page the following properties can be set:

| Property         | Description                     | Values                             |
| :---             | :---                            | :---                               |
| imageSource      | Image of the current page       | URL, Resource Identifier or String |
| title            | Title of the current page       | Resource Identifier or String      |
| text             | Body text of the current page   | Resource Identifier or String      |
| footer           | Footer text of the current page | Resource Identifier or String      |
| nextButtonTitle  | Text of the next button         | Resource Identifier or String      |
| prevButtonTitle  | Text of the previous button     | Resource Identifier or String      |
| termsButtonTitle | Text of the terms button        | Resource Identifier or String      |
| link             | The (deep) link to the terms    | URL                                |

Each view will be hidden when not set.

**Please note:**

1. The `prevButtonTitle` can only be used for back navigation.
2. When only one button is set, it will use the full width.
3. When name of the `termsButtonTitle` is set, the termsLink is mandatory.

### Usage

You can simply use the onboarding by setting the destination in your navigation components. Add the argument model which
is later used to pass the configuration for your onboarding.

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

### Terms and privacy custom setup

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
        app:argType="reference" />
    <argument
        android:name="imagePath"
        android:defaultValue="/android_asset/ic_terms_header.png"
        app:argType="string" />
    <argument 
        android:name="headerTitle"
        android:defaultValue="@string/Onboarding.Terms.introText"
        app:argType="reference" />
    <deepLink
        android:id="@+id/deepLink"
        app:uri="my.app://terms" />
</fragment>
```

**Required**

1. The argument `resId` needs to be set as shown in the example, to display at least the Terms or privacy.
2. The deeplink need to be set to handle the given Uri. The Uri given in the configuration need to match the deeplink to
   navigate to the terms fragment. optional: imagePath and headerTitle are optional arguments. If set they will be
   displayed as header on Top of the terms. VectorDrawables in the resources are also supported.

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

## Shopfinder

### Configuration

For the shop finder and the details page the location tracking needs to be started as soon as permission is granted.
You can either start location tracking directly via an instance of the location manager or with the snabble check in manager if used.
```kotlin
LocationManager.getInstance(this).startTrackingLocation()
Snabble.checkInManager.startUpdating()
```

You can stop the tracking over the same stop method.

### Setup

To setup the shop finder:

1. Navigate to the `ShopListFragment` and set it up in your Navigation file:

    ```xml
    <fragment
        android:id="@+id/navigation_shops"
        android:name="io.snabble.sdk.shopfinder.ShopListFragment"
        android:label="Your label" />
    ```

    Navigate to your destination:

    ```kotlin
    navController.navigate(R.id.navigation_shops)
    ```

2. Set it up as part of a `NavigationBar`: in your menu file set up the destination for the tab

    ```xml
    <item
        android:id="@id/navigation_shops"
        android:icon="@drawable/your_icon"
        android:title="Your title" />
    ```

3. Execute the SnabbleUi-Toolkit event to start the shop finder

    ```kotlin
    SnabbleUiToolkit.executeAction(context,SnabbleUiToolkit.Event.SHOW_SHOP_LIST)
    ```

4. Extend the 'ShopListFragment' to implement custom behaviour (e.g back button for toolbar etc.)

    The details page for each shop opens on click by default. If further customizations need to be done
    you can extend the 'ShopDetailsFragment' and navigate to the new destination by overwriting the
    SnabbleUi-Toolkit event `SHOW_SHOP_LIST_DETAILS`:

    ```kotlin
    SnabbleUiToolkit.setUiAction(context, SnabbleUiToolkit.Event.SHOW_DETAILS_SHOP_LIST) { _, args ->
        navigate(R.id.navigation_shops_details, args)
    }
    ```

### Customization

You can set up a Toolbar title by overwriting the following strings

```xml
<resources>
   <string name="Snabble.Shop.Finder.title" />
   <string name="Snabble.Shop.Detail.title" />
</resources>
```

By default the shop list title is set to "shops" and the details page takes the store name as title.

#### Optional

You can set up button which only appears after the check in. 
To set up the button overwrite the following string
```xml
<resources>
    <string name="Snabble.Shop.Detail.shopNow" />
</resources>
```

To set up an event for the button click set up an ui action for the 'SHOW_DETAILS_BUTTON_ACTION' event

```kotlin
SnabbleUiToolkit.setUiAction(context, SnabbleUiToolkit.Event.SHOW_DETAILS_BUTTON_ACTION) { _, _ ->
    // your action
}
```

## DynamicView

### JSON Configuration

The description of a **Dynamic View** is done via a JSON file. The file must contains two elements `configuration` and `widgets`.

```json
{
	"configuration": {
	    ...
	},
	"widgets": [
	   ...
	]
}
```


#### Configuration

The `configuration` element use the `image` to display as a background for a **Dynamic View**.
The `style` element can be left empty since its only used for ios configuration.

| Parameter     | Type         | Required    | Example                                                |
|---------------|--------------|-------------|--------------------------------------------------------|
| `image`       | `String?`    | no          | `"Dashboard/teaser"`                                   |
| `style`       | `String?`    | no          | `"scroll"` or `"list"` or `""`                         |
| `padding`     | `Margins?`   | no          | `[ 30, 16 ]`                                           |

The `padding` can set as: `all` = `[8]`, `horizontal/vertical` = `[ 30, 16 ]` or explicit as `left`, `top`, `right`, `bottom`

### Widgets

The widgets are defined as an array of `"widgets"` in the [JSON Configuration](#configuration) file. Currently the following widget types are supported:

#### User Widgets

The [**User Widgets**](#user-widgets) are used to represent App specific UI elements and actions the App can respond to. The hosting app can return requested UI elements to display. Each widget send an action to the hosting App when the user taps on it.

* [Text](#text)
* [Image](#image)
* [Button](#button)
* [Information](#information)
* [Toogle](#toggle)
* [Section](#section-widget)
* [SwitchEnvironment](#switch-Environment)

### SDK Action Widgets

The [**SDK Action Widgets**](#sdk-action-widgets) are special widgets build into the SDK. These widgets are self contained and implements a predefined action to trigger, if the use taps on it.

* [Location Permissions](#location-Permission)
* [Start Shopping](#start-Shopping)
* [Show All Stores](#all-Stores)
* [Connect Wlan](#connect-Wlan)
* [Customer Card](#customer-Card)
* [Last Purchases](#last-Purchases)
* [Developer Mode](#developer-Mode)
* [Version](#version)
* [AppUserId](#appUserId)
* [ClientId](#clientId)



## Widget Reference

Each Widget has three fixed parameters.

| Parameter     | Type         | Required    | Example                                                |
|---------------|--------------|-------------|--------------------------------------------------------|
| `id`          | `String`     | yes         | `"1"`                                                  |
| `type`        | `String`     | yes         | `"text"`                                               |
| `padding`     | `Margins`    | yes         | `[ 30, 16 ]`, `{ "bottom": 4 }`                        |

The `id` parameter is a `String` which must be unique for any siblings in a group of widget definitions.
The `type` parameter must be one of the Widget types defined below.
The padding parameter describes the margin around the widget.

## Widget interactions

Interactions are passed to the app via the widget ID.
The widget id is always the id from the configuration and should be unique for each widget.

The Dynamic View returns a DynamicAction object for each Widget click containing the widget object and info about the widget as Map<String, Any>?.

## User Widgets

### Text

Widget to display text.

| Parameter        | Type         | Example                                                           |
|------------------|--------------|-------------------------------------------------------------------|
| `id`             | `String`     | `"1"`                                                             |
| `type`           | `String`     | `"text"`                                                          |
| `text`           | `String`     | `"Sample.Dashboard.text"`                                         |
| `textColor`      | `String?`    | `"label"` (semantic color)                                        |
| `textStyle`      | `String?`    | `"title"` (semantic textStyle)                                    |
| `showDisclosure` | `Bool?`      | `true`    (only ios)                                              |

### Image

Widget to display an image.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"image"`                                                            |
| `image        | `String`     | `"Sample.Dashboard.image"`                                           |

### Button

Widget to display a button.

| Parameter          | Type         | Example                                                         |
|--------------------|--------------|-----------------------------------------------------------------|
| `id`               | `String`     | `"1"`                                                           |
| `type`             | `String`     | `"button"`                                                      |
| `text`             | `String`     | `"Sample.Dashboard.text"`                                       |
| `foregroundColor`  | `String?`    | `"label"` (semantic color)                                      |
| `backgroundColor ` | `String?`    | `"systemBackground"` (semantic textStyle)                       |

### Information

Widget to display an optional image on the left side of an informational text.


| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"information"`                                                      |
| `text`        | `String`     | `"Sample.Dashboard.Information.text"`                                |
| `imageSource` | `String?`    | `"Sample.Dashboard.Information.image"`                               |

### Toggle

Widget to display an text on the left side of an toggle button.


| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"toggle"`                                                           |
| `text`        | `String`     | `"Profile.showOnboarding"`                                           |
| `key`         | `String`     | `"io.snabble.sample.showOnboarding"`                                 |

The toogle feature is bound to the 'key' and should be unique.

### Section

The Section Widget help to group widgets in section.


| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"section"`                                                          |
| `header`      | `String`     | `"Profile.header"`                                                   |
| `items`       | `[Widget]`   | Contains an array of child widgets                                   |


## SDK Action Widgets

The *SDK Action Widgets* are special widgets build into the SDK.
These widget are designed to fulfill a specific task.

### Location Permission

Widget to ask the user to grant for location permissions. After the user has approved access the widget will disappear.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"snabble.locationPermission"`                                       |

### Start Shopping

If the user has entered a shop, a tap on the `Start Shopping` button will start the scanner.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"snabble.startShopping"`                                            |

### All Stores

The widget navigates to the Shop Finder view where all shops are listed. The user can select a specific item of the list and the Shop Detail view with a map, shop description and opening hours is displayed.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"snabble.allStores"`                                                |

### Connect Wlan

Widget to ask the user to grant access to a local Wifi network.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"snabble.connectWifi"`                                              |

### Customer Card

Widget to show a customer card.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"snabble.customerCard"`                                             |
| `text`        | `String`     | `"Sample.customerCard.description"`                                  |
| `image        | `String`     | `"Sample.CustomerCard.image"`                                        |

### Last Purchases

Widget to display one or two of the recent purchases.
This widget return two values: `more` and `purchases`.

On tap on a receipt the matching id can be handled via the info object of the purchase object
as shown in the example below.
If the user taps on a recipe preview item, the detail view for that receipt is can be shown.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"snabble.lastPurchases"`                                            |

#### The following shows an example how to handle purchases events:

```Kotlin
        when (action.widget.id) {
            "lastPurchases" -> {
                when (action.info?.get("action")) {
                    "more" -> //To something
                    "lastPurchases" -> {
                        (action.info?.get("id") as? String)?.let {
                            // Do something with the specific purchase id
                        }
                    }
                }
            }
            else -> Unit
        }
    }
```

### Developer Mode

Widget to display the developer options.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"io.snabble.developerMode"`                                         |
| `type`        | `String`     | `"snabble.devSettings"`                                              |
| `text`        | `String`     | `"Profile.developerMode"`                                            |


### Switch Environment

Widget to switch the system environment.


| Parameter     | Type          | Example                                                              |
|---------------|---------------|----------------------------------------------------------------------|
| `id`          | `String`      | `"io.snabble.environment"`                                           |
| `type`        | `String`      | `"snabble.switchEnvironment"`                                        |
| `text`        | `String`      | `"Profile.environment"`                                              |
| `"values"`    | `[(id,text)]` | An array of tuples where the id is assigned to the multiValue id.    |

#### The following shows an example SwitchEnvironment-JSON definition:

```json
{
	"type": "snabble.switchEnvironment",
    "id": "io.snabble.environment",
    "text": "Profile.environment",
    "padding": [16, 0],
	"values" : [
		{
			"id": "io.snabble.environment.testing",
			"text": "Profile.Environment.testing"
		},
		{
			"id": "io.snabble.environment.staging",
			"text": "Profile.Environment.staging"
		},
		{
			"id": "io.snabble.environment.production",
			"text": "Profile.Environment.production"
		}
	]
}
```

### Version

Widget to display the current version and to switch on developer mode.
The implementation of the developer mode activation is delegated to the hosting application.

To activate the developer mode this widget can be used with the DevSettingsLoginFragment.<br>
If used via the `DevSettingsLoginFragment().show()` function the SDK asks the hosting app for a base64 decoded string with the following id: `"dev_settings_password"`.
The hosting app can provide a app specific password to enable developer mode.
If the hosting app return `null` the default password `Password` is used to enable developer mode.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"1"`                                                                |
| `type`        | `String`     | `"version"`                                                          |

### AppUserId

Widget to show the current AppUserId. On click copies the AppUserId to the clipboard.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"appUserId"`                                                        |
| `type`        | `String`     | `"snabble.appUserId"`                                                |

### ClientId

Widget to show the current ClientId. On click copies the ClientId to the clipboard.

| Parameter     | Type         | Example                                                              |
|---------------|--------------|----------------------------------------------------------------------|
| `id`          | `String`     | `"clientId"`                                                         |
| `type`        | `String`     | `"snabble.clientId"`                                                 |


## Screens

The following Screens can be setUp with just providing the matching config as json file.
The config has to be provided via the assets directory.

| Screen        | config name         | Type                                                   |
|---------------|---------------------|--------------------------------------------------------|
| `home`        | `homeConfig`        | `"json"`                                               |
| `profile`     | `profileConfig`     | `"json"`                                               |
| `devsettings` | `devSettingsConfig` | `"json"`                                               |

### Setup Example

JSON Example of SnabbleSampleApp file `homeConfig.json` to describe the **Start** tap (Home) as a **Dynamic View**.

```json
{
  "configuration": {
    "image": "home_default_background",
    "style": "scroll",
    "padding": [0]
  },
  "widgets": [
    {
      "type": "text",
      "id": "text1",
      "text": "Dashboard.title",
      "textColor": "snabbleBlue",
      "textStyle": "title",
      "padding": [0, 16, 5, 0]
    },
    {
      "type": "text",
      "id": "text2",
      "text": "Dashboard.text",
      "textStyle": "body",
      "padding": [0, 0, 5, 0]
    },
    {
      "type": "snabble.locationPermission",
      "id": "locationPermission",
      "padding": [0, 8, 5, 0]
    },
    {
      "type": "snabble.startShopping",
      "id": "startShopping",
      "padding": [0, 0, 5, 0]
    },
    {
      "type": "snabble.allStores",
      "id": "allStores",
      "padding": [0, 0, 5, 0]
    },
    {
      "type": "information",
      "id": "information",
      "text": "Füge eine Kundenkarte hinzu",
      "image": "customer_card",
      "padding": [0, 6, 16, 0]
    },
    {
      "type": "snabble.connectWifi",
      "id": "wifi",
      "padding": [0, 6, 16, 0]
    },
    {
      "type": "snabble.lastPurchases",
      "id": "lastPurchases",
      "projectId": "snabble-sdk-demo-app-oguh3x",
      "padding": [0]
    }
  ]
}
```
### Interactions Example

The interaction can be handled via the matching viewModels.

| Screen        | ViewModel              | Type                                                   |
|---------------|------------------------|--------------------------------------------------------|
| `home`        | `homeViewModel`        | `"DynamicViewModel"`                                   |
| `profile`     | `profileViewModel`     | `"DynamicViewModel"`                                   |
| `devSettings` | `devSettingsViewModel` | `"DynamicViewModel"`                                   |

#### The following shows an example how to handle widgets events with the given viewModel:

```Kotlin
    private val homeViewModel: DynamicHomeViewModel by viewModels()

    homeViewModel.actions.asLiveData().observe(this, ::handleHomeScreenAction)

    private fun handleHomeScreenAction(action: DynamicAction) {
        when (action.widget.id) {
            "startShopping" -> navBarView.selectedItemId = R.id.navigation_scanner
            "purchases" -> {
                when (action.info?.get("action")) {
                    "more" -> SnabbleUiToolkit.executeAction(context = this, SHOW_RECEIPT_LIST)

                    "lastPurchases" -> {
                        (action.info?.get("id") as? String)?.let {
                            lifecycleScope.launch {
                                showDetails(this@MainActivity.findViewById(android.R.id.content), it)
                            }
                        }
                    }
                }
            }
            else -> Unit
        }
    }
```
