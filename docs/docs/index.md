# Introduction
Offer Scan & Go to your customers in your own app – with the Snabble Mobile SDK. Our mobile SDK supports the entire range of Snabble base functionality including scanning, product lists, promotions, payment and shops.
The SDK covers the whole communication with the Snabble Cloud-Platform and provides you with UI elements for your app that are easy to use and customize, so that you simplify the integration of Scan & Go into your app significantly.
For details about integrating with the Snabble Cloud-Platform, please see the [Platform Documentation](https://github.com/snabble/docs) on GitHub.

## Distribution & Download
### iOS
* [Snabble Mobile SDK for iOS on GitHub](https://github.com/snabble/iOS-SDK)
* Supported iOS versions: iOS 12+

### Android
* [Snabble Mobile SDK for Android on GitHub](https://github.com/snabble/Android-SDK)
* [Sample Apps](sample-apps.md)
* Supported Android versions: Android 8+

## Feature Overview
### Scanning Products
The SDK comes with a standard scanner functionality and the ability to match the scanned data to products, coupons or even deposits. The product data is provided through the SDK with great performance and includes all necessary product details also covering special cases like age verification, multiple taxation options or product related discounts and promotions.
With this enriched product data and the provided SDK UI elements it is very simple to integrate a fully functional product scanner into your app.
### Offline Synchronisation
When entering a store with an app using the Snabble Mobile SDK, product data is downloaded into the app. This not only allows for a much faster scan result, but more importantly, it allows for scanning in areas of the store where no connection to the internet is available. After the purchase, product data is kept up-to-date via intelligent delta updates.
### Cart
The SDK comes with ready to use UI for the shopping cart that is already integrated into the scanner view. The cart handles not just adding and removing products but also synchronization with the Snabble Cloud-Platform. In this way the cart is automatically processed and updated by Snabble Cloud-Platform components like a promotion engine.
In this way the cart always shows correct pricing information considering applicable coupons, manual discounts, bundles or similar promotion logic.
The SDK UI elements for the cart already include visual representation of special cart items like 18+ products or applied promotions.
### Promotions
The Snabble Cloud-Platform already provides a powerful promotion engine to manage promotions. If you choose to use your own system to manage promotions this system will be integrated into the Snabble Cloud-Platform in advance. In all cases the SDK provides you with ready to use functionality for promotions.
There are two main promotion functions provided through the SDK:
manual discounts
manual discounts that can be selected by the customer during the scanning process. The SDK UI elements for the product scanner contain elements for the selection of manual discounts if these are configured in the promotion engine.
shopping cart calculation
automated calculation and updates of the shopping cart using the promotion logic attached to the Snabble Cloud-Platform. The visual representation of applied promotions is already included in the SDK UI elements for the shopping cart.
### Loyalty Cards
Loyalty Cards can be added to the app by scanning or entering the information using the keyboard. Added loyalty cards are then automatically applied to each checkout.
<!--
### Empties And Deposits
### Vending Machine Output
### Checkout Options
Lorem ipsum
-->
### Payment
Using the Snabble Mobile SDK, your app is able to handle payments with the Payment Service Providers Fiserv (First Data, Telecash), PayOne and Datatrans. You choose which payment methods you want to offer to your customers. These payment methods are currently supported by our Snabble Mobile SDK:

#### Fiserv (First Data, Telecash)
 * SEPA Direct Debit
 * Credit Cards (VISA, MasterCard, American Express)
 * Apple Pay
 * Google Pay
#### PayOne
 * SEPA Direct Debit
 * Credit Cards (VISA, MasterCard, American Express)
#### Datatrans
 * Postfinance
 * Twint

### Offline Payment
The Snabble Mobile SDK is also able to generate a QR code containing the cart information. This QR code can be scanned by your PoS/Cashdesk. Payment is then handled by your PoS.
### Shop List And Details
A list of available shops is downloaded from the Snabble Cloud-Platform and shown in a table list inside the app. If the user has allowed to use geolocation, the shops are sorted by distance to the user. Also, a detail view of each shop is available, including a map view.

