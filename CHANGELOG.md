# Changelog  
All notable changes to this project will be documented in this file.

## [0.47.2]

### Fixed
- Initialization error

## [0.47.1]

### Added
- Support for localized coupons

## [0.47.0]

### Changed
- Updated build tools to 7.0.1
- Updated firebase detector to use new mlkit on-device api's

## [0.46.0]

### Added
- Translations for fr and it

## [0.45.2]

### Fixed
- CheckoutOnlineView not updating when coming from an activity resumed state

## [0.45.1]

### Changes
- Upgraded Datatrans SDK to v1.4.2

### Fixed
- Fixed Datatrans Transaction Listener

## [0.45.0]

### Added
- Added API to get redeemed coupons 

## [0.44.4]

### Added 
- Added button themes for overriding button themes.

### Fixed
- Handle edge case when scanning a user weighed product with a pre weighed barcode
- Fixed an endless loop that resulted in the app freezing

## [0.44.3]

### Fixed 
- Fixed visual bug in ShoppingCartView when deleting Coupons

## [0.44.2]

### Fixed
- Removed usage of removeIf, to support API < 26 without using coreLibraryDesugaring

## [0.44.1]

### Added
- Added coupon fields for colors and disclaimer

## [0.44.0]

### Added
- Added support for DIGITAL coupons with additional fields

## [0.43.2]

### Added
- Added an icon to the payment credentials list empty state

### Fixed
- PaymentOptionsView not updating when adding a credit card via datatrans

## [0.43.1]

### Fixed
- Persist shopping list on new uuid's

## [0.43.0]

### Added
- Added support for taxation

## [0.42.2]

### Fixed
- Fixed NullPointerException introduced in 0.42.1

## [0.42.1]

### Fixed
- Only update prices when the shop differs, not always when setCheckedInShop is called

## [0.42.0]

### Changed
- Removed manual discount message
- Added in memory lru cache for asset decoding
- Added async parameter for asset loading

## [0.41.0]

### Added
- Added support for payment method descriptors
- Added support for credit card payment using Datatrans

### Fixed
- Fixed internal storage directory sometimes pointing to null

## [0.40.2]

### Fixed
- Fix crash when google pay gateway is not configured correctly, showing error message instead

## [0.40.1]

### Fixed
- Fixed SelfScanningFragment being out of sync with SelfScanningView

## [0.40.0]

### Breaking Changes
- Project.getShops now returns a List instead of an Array

### Added
- Added support for "activeShops"

### Fixed
- Fixed PostFinance and Twint not selectable when skipping over project specific payment methods

## [0.39.13]

### Fixed
- Fixed crash when using standalone PaymentCredentialsListView

## [0.39.12]

### Added
- Telemetry for shopping list tags

## [0.39.11]

### Fixed
- ShoppingCart items not updating correctly when only the manual coupon state changes

## [0.39.10]

### Changed
- Remove dependency on gitter and migrate it to maven central.

## [0.39.9]

### Fixed
- Fixed crash on adding a second payment method on the same project

## [0.39.8]

### Fixed
- Removed additional padding on Snackbars when using Android 11 + OnApplyWindowInsetListener

## [0.39.7]

### Fixed
- Reversed camera torch drawable

## [0.39.6]

### Added
- Added support for "manualDiscountFinalCode"

## [0.39.5]

### Fixed
- QR codes being cutoff when maxSizeMM is higher than available view space
- Payment methods being shown to project that are not part of the project

### Updated
- Updated Datatrans SDK to v1.4.1

## [0.39.4]

### Added
- Add configuration flag `isUsingShoppingList` to avoid creating a FTS when not required

## [0.39.3]

### Fixed
- Fixed checkouts not aborting when age verification check is not passing

## [0.39.2]

### Fixed
- Added package query for Android 11+

## [0.39.1]

### Added
- Jump to play store if google pay is not installed

## [0.39.0]

### Added
- Added support for Google Pay

### Fixed
- Fixed exceptions when using coupons that affect the whole cart

## [0.38.1]

### Updated
- Updated Datatrans SDK to v1.4.0

### Fixed
- Product searches of sku's are now also working on pressing the imeActionDone.

## [0.38.0]

### Added
- Added stackable scan messages with a new Style

### Deprecated
- Deprecated UIUtils.showTopDownInfoBox

## [0.37.12]

### Added
- Added indicator if an manual discount was applied or not

### Fixed
- Fixed pull to refresh in ShoppingCartView
- Fixed selecting manual discounts resetting amount, even when no item is already in cart
- Products with manual discounts are now editable

## [0.37.11]

### Fixed
- Fixed crash when no TWINT or PostFinance payment method could be added

## [0.37.10]

### Changed
- Changed default Datatrans environment to Production

### Fixed
- Fixed listing of TWINT and PostFinace Card payment methods when no credit card payment
methods are available

## [0.37.9]

### Changed
- Made credit card 3d secure hint locale aware

## [0.37.8]

### Fixed
- Only update cart when it actually changed

## [0.37.7]

### Added
- Support for switching Datatrans environments
- CreditCardInputView's 3d secure hint now used project specific i18n

## [0.37.6]

### Added
- Added option to vibrate when item was added to the cart

## [0.37.5]

### Fixed
- Fixed price override codes

## [0.37.4]

### Changed
- Not showing cart restoration after successful online checkout

## [0.37.3]

### Added
- Added age restriction indicator in shopping cart items

## [0.37.2]

### Fixed
- Fix VISA credit card input not opening

## [0.37.1]

### Fixed
- TWINT and PostFinance payment methods are now listed in their project specific lists

## [0.37.0]

### Added
- Support for transmission templates
- Added support for TWINT and PostFinance Payments using Datatrans
- searchByCode now also searches in SKU's

### Changes
- Increased the minimum required database revision to 1.25

## [0.36.0]

### Added
- Support for printed coupons

### Changed
- Support for new coupon metadata
- Improved SEPACardInputView for copy pasting IBAN's with prefixes
- Removed flash and search buttons in SelfScanningView
- Replaced scan indicator with a barcode image

### Fixed
- Remove old scan result messages

## [0.35.9]

### Fixed
- Fixed amount for manual coupons 
- Fixed listener leak in CheckoutBar

## [0.35.8]

### Changes
- Add option to set project of the ProductResolver
- Changed builder method name from `setBarcodeOfProject(...)` to `setBarcode(...)` to simplify usage

## [0.35.7]

### Changes
- Extend telemetry for shopping lists

## [0.35.6]

### Changes
- Improved performance of FTS4 index creation
- Improved layout of Scan Dialog

### Fixes
- Fixed line item price being ignored in default cases

## [0.35.5]

### Fixed
- Removed unnecessary padding in SEPACardInputView

## [0.35.4]

### Fixed
- The priceHeight property of the CheckoutBar contains now the outer margin

## [0.35.3]

### Changes
- Migration ProductResolver to kotlin and make it possible to use it without a chooser
- The show() method was deprecated and replaced by resolve() since it is possible now to use the
  resolver without visible interaction
- Empty product image urls will be converted to null
- The priceHeight property of the CheckoutBar contains now its margin

## [0.35.2]

### Added
- Support for manual coupons

### Fixed
- Long messages being cut off when scanning

## [0.35.1]

### Changes
- Migration of dependencies to mavenCentral and jitpack

## [0.35.0]

### Changes
- Various structural changes

## [0.34.4]

### Changes
- Improved visuals on PaymentOptionsView

## [0.34.3]
 
### Fixed
- Fixed night mode confusion in not preloaded assets
- Update CameraX to 1.0.0-rc04 to fix a camera initialization crash on Samsung Galaxy S20 Ultra 5G

## [0.34.2]

### Fixed
- Fix a crash in PaymentOptionsView when a project has no brand

## [0.34.1]

### Fixed
- Products with multiple scanned codes now are using the default template when selected from a bundle

## [0.34.0]

### Added
- Add support for JPG and WEBP assets

### Fixed
- Fix vPOS updates with weight information resulting in incorrect quantities

## [0.33.11]

### Fixed
- Leak of SelfScanningView in SelfScanningFragment when in onResume()
- Camera black screen on Google Pixel 4a when using multi-window mode
- Improved camera loading behaviour when calling BarcodeScannerView.start() repeatedly

## [0.33.10]

### Fixed
- Endless recursion when handling unknown gs1 codes

## [0.33.9]

### Fixed
- Fixed incorrect display of units when processing line items from vPOS

## [0.33.8]

### Fixed
- Added more missing keyguard checks

## [0.33.7]

### Added
- Added explanation text for checkout online status screen

### Fixed
- Fixed a crash when adding a new payment method from the ShoppingCart
- Fixed scan dialog update button being cutoff when using the highest font scaling setting
- Fixed added missing keyguard after payment selection

## [0.33.6]

### Fixed
- Fixed a bug that aborted terminal payments could not be retried

## [0.33.5]

### Changed
- Use company name in 3d secure hint

## [0.33.4]

### Changed
- i18n updates

### Fixed
- Fixed crash when not providing a type to PaymentCredentialsListView

## [0.33.3]

### Fixed
- Require screen lock for payment methods that require credentials when calling from the shopping cart
## [0.33.2]

### Fixed
- Require screen lock for adding secure payment methods

## [0.33.1]

### Fixed
- Updated picasso dependency, to resolve breaking changes since 2.5

## [0.33.0]

### Fixed
- PaymentOptionsView not always updating its count
- Now using a subclassed FileProvider to avoid clashes with other libraries

## [0.33.0-beta04]

### Changes
- Mark all kotlin fragments as open

## [0.33.0-beta03]

### Changes
- Small bugfixes

## [0.33.0-beta02]

### Fixed
- PaymentOptionsView not always updating
- Improved layout of PaymentOptionsView

## [0.33.0-beta01]

### Breaking Changes
- Added support for PSD2 credit cards. This requires that existing credit cards are invalidated, which
can be checked using PaymentCredentialsStore.hasRemovedOldCreditCards()
- 2 new views and there respective fragments related to the new credit card flow have been added: PaymentOptionsView and ProjectPaymentOptionsView.
ProjectCredentialsListView should not be used as a Overview anymore, the new recommended entry point is now PaymentOptionsView.
- SnabbleUI.Callback now provides data as a Bundle. This bundle is mostly used for fragment arguments.
- EVENT_PRODUCT_CONFIRMATION_HIDE now provides its ShoppingCart.Item in the Bundle as a Serializable "cartItem".

## [0.32.11]

### Fixed
- Fixed translation error in qr code checkout screen

## [0.32.10]

### Added
- Added an error message when a deposit return voucher is already redeemed

### Fixed
- Fixed a crash when scanning deposit return vouchers

## [0.32.9]

### Fixed
- Fixed another crash in BarcodeScannerView

## [0.32.8]

### Fixed
- Fixed crash in BarcodeScannerView

## [0.32.7]

Identical to 0.32.6, release because of issues with Github Packages

## [0.32.6]

### Fixed
- React-Native incompatibility with new BarcodeScannerView implementation

## [0.32.5]

### Changes
- Mark BarcodeScannerView as 'open', for compatibility with previous SDK releases

## [0.32.4]

### Changes
- Updates translations

### Fixed
- Auto focus not always working as expected
- Checkout now waits for Exit-Code completion

## [0.32.3]

### Fixed
- Now using lineItem.amount instead of lineItem.count

## [0.32.2]

### Fixed
- Deep search for mergeable cart items
- Fix wrong add to cart button message when adding a non-mergeable item

## [0.32.1]

### Fixed
- BarcodeScannerView is now paused when initialized after already being paused

## [0.32.0]

### Major Changes
- Completely rewritten BarcodeScannerView. It now uses the Camera2 API via CameraX. The API surface did not change.

### Added
- Added translations for German, Hungarian and Slovakian languages.

### Fixed
- Products with specified quantities are not mergable anymore

## [0.31.4]

### Fixed

- Crash when database and metadata code templates mismatch

## [0.31.3]

Rerelease of 0.31.2 due to github packages conflict

## [0.31.2]

### Fixed
- Unit display when scanning GS1 codes
- Use original code when sending codes scanned from GS1 codes to backend

## [0.31.0]

### Added
- Support for Brands

## [0.30.1]

### Added
- Support for UPC-A codes in EAN13 or EAN14 codes

### Changed
- Prioritize online payment methods over offline methods if available
- Route to entering payment method if not available

### Fixed
- Fixed shopping cart not generating a new id after successful checkout

## [0.30.0]

### Added
- Support for GS1 Barcodes

## [0.29.8]

### Changed
- Update hashes used in certificate pinning

## [0.29.8]

### Changed
- Add support for lets encrypt certificates on Android versions before 7.1.1

## [0.29.7]

- More vendor specific strings

## [0.29.6]

### Changed
- Support vendor specific strings for salestop

## [0.29.5]

### Fixed
- Fixed some regressions in price display since 0.29.4

## [0.29.4]

### Added
- Add support for vPOS price reevaluation codes

## [0.29.3]

### Fixed
- Grey out checkout button when no online checkouts are available and no offline fallback is configured

## [0.29.2]

### Changed
- Now using PUT and GET instead of POST and a client-side generated uuid for checkout process to be able to reuse
existing checkout processes

## [0.29.1]

### Fixed
- Fixed a potential crash when updating Assets
- Fixed a potential crash when payment methods are not valid anymore and needed to remove

## [0.29.0]

### Breaking Changes
- Sealed inner classes of Shop, you may need to use getter functions instead of accessing the 
fields directly

### Added
- Support for deposit return vouchers's
- Parse company data from Shop
- Parsing of terms of use data
- Support for exit gate's
- Support for performedBy in checks

### Fixed
- Projects that have no offline fallback as a payment method are now forced to enter
payment credentials
- Fixed age restriction hint promting even when product dialog got dismissed

## [0.28.1]

### Fixed
- Stop polling for payment origin candidates on 404
- Product that were scanned with encoded data are now not showing the bundle dialog anymore

## [0.28.0]

### Added
- Support for updating the users age by external payments
- Added age restricted product warning message

### Changed
- Route to PAYMENT_ABORTED on terminal aborts
- QRCode offline layout improvements 

### Fixed
- Fixed BarcodeScannerView choosing the wrong camera when multiple back cameras
are available and not all camera support auto-focus
- Fixed NullPointerException on unknown fulfillment check state
- ProgressBar not showing when payment is processing on gatekeeper checkout

## [0.27.3]

### Fixed 
- Fix a initialization crash in BarcodeScannerView
- Handle obscured urls gracefully in TokenRegistry

## [0.27.2]

### Fixed
- Fixed a crash when parsing metadata contain null payment methods

## [0.27.1]

### Fixed
- Fixed a possible freeze in BarcodeScannerView

## [0.27.0]

### Added
- Added support for normalized coordinates in ScanIndicatorView

## [0.26.7]

### Changes
- Now using lower camera preview resolution when device memory is low

### Fixed
- Fixed a bug that caused products to be scannable even when they are not available

## [0.26.6]

### Fixed
- Fixed checkout ProgressBar sometimes being endless 

## [0.26.5]

### Changes
- Faster call timeout for checkout request that could be handled offline

## [0.26.4]

### Changes
- Articles can now not go below quantity of 1 when scanning
- Fixed a crash when handling old metadata 

## [0.26.3]

### Changes
- Show fallback scan message when invalid scan message is set 

## [0.26.2]

### Changes
- Improved CheckoutOffline layout

## [0.26.1]

### Added
- Added support for multiple pricing categories in ProductDatabase

## [0.26.0]

### Changes
- Requires schemaVersion 1.21

### Added
- Added support for 'notForSale'

## [0.25.7]

### Fixed
- Fixed customerNetwork data model - breaking change from 0.25.6

## [0.25.6]

### Changes
- Added "animateBarcode" property to BarcodeView
- Added customerNetworks to Shop
- Allow receipt pdf url to be null

## [0.25.5]

### Fixed
- Do not require keyguard authentication for employee card payment methods

## [0.25.4]

### Added
- Support for "displayNetPrice"

### Fixed
- CUSTOMER_CARD_POS payment method not displaying in payment selector

## [0.25.3]

### Fixed
- Auto-approval payment methods should now also show a progress dialog if they take too long
- Payment method that were added in other App-Id's are now excluded when populating the list
- Fixed paydirekt not going back after adding new credentials
- Fixed age verification not always popping up in new payment selection

## [0.25.2]

### Fixed
- Fixed a crash when pausing the activity while the payment selection is open
- Fixed payment select dialog not always selecting the current payment
- Fixed most http requests calling the error callback when being cancelled

## [0.25.1]

### Fixed
- Payment methods that are not requiring a intermediate status screen are now showing the payment
success screen
- The OnFullfillmentUpdateListener is now not emitting onFullfillmentDone when no fulfillments are present
after a successful payment
- Payment selector is now hiding customer card payment methods that are not added
- Fixed a crash in the payment selector when adding a new customer card

## [0.25.0]

### Added
- Support for paydirekt

## [0.24.7]

### Fixed
- Now creating a new scanned code when selecting a bundle product
- vPOS price display now multiplies by units, if available
- vPOS replace operations with products that are not in the local db are not blocking the
ui anymore

## [0.24.6]

Is identical to 0.24.5

## [0.24.5]

### Fixed
- Fixed a crash while generating the list of available payment methods

## [0.24.4]

### Added
- Added checkmark for selected payment method

### Changed
- Automatically select payment method added from shopping cart

## [0.24.3]

### Fixed
- Don't show payment credentials of other app id's in new payment selector
- Skip payment method list and go directly to bottom sheet when adding payment methods from
shopping cart

## [0.24.2]

### Fixed
- Payment selection now does hide completely when showing empty state
- Price does now update correctly when undoing items from empty state
- Now using Snabble.Shoppingcart.numberOfItems and Snabble.Shoppingcart.numberOfItems.one

## [0.24.1]

### Changed
- Include okhttp version in User-Agent, this requires at least okhttp 4.7.0.

## [0.24.0]

### Added
- Moved payment selection to shopping cart
- SnabbleUI.getProjectAsLiveData to get the current project as a reactive element

### Removed
- PaymentSelectionView and PaymentSelectionFragment

## [0.23.2]

### Added
- Support for availabilities

### Fixed
- Hide editing controls for products with embedded data
- Fixed a rare crash when dismissing items in the shopping cart

## [0.23.1]

### Changed
- Make checkoutProcess accessible after payment failure

## [0.23.0]

### Added
- Support for fulfillment's

## [0.22.4]

### Changed
- Check employee card payment methods for validity

### Fixed
- Its now possible to enter employee cards without access to the Android KeyStore

## [0.22.3]

### Fixed
- Fix crash on BarcodeScannerView introduced in 0.22.0

## [0.22.2]

### Fixed
- Allow setAppUserBase64 to be null to clear the current app user

## [0.22.1]

### Fixed
- Update appUser url

## [0.22.0]

### Added
- Added support for checkout process checks (age verification)

### Changed
- Show progress indicator when payment is processing
- Age verification is not optional anymore
- Removed enableAgeVerification config parameter

### Fixed
- Properly handle camera state when rapidly creating / destroying multiple copies of BarcodeScannerView from a thread that is not the main thread
- Only let the user add payment methods that are available to the app

## [0.21.5]

### Changed
- Exclude not used assets from asset preload

## [0.21.4]

### Changed
- Canceling a checkout by pressing cancel now goes back in the navigation hierarchy instead of showing a payment error message

## [0.21.3]

### Added
- Added events for product confirmation show/hide

### Changed
- Now listing added employee cards in payment method list

### Fixed
- Added workaround to fix ProductSearchView not updating when embedding in react-native applications

## [0.21.2]

### Fixed
- Invalidate tokens on new app user id

## [0.21.1]

### Fixed
- Fixed setAppUserIdBase64

## [0.21.0]

### Added
- Support for app user id's
- Support for American Express
- Limited support for german age verification

### Changed
- Now transmitting obfuscated credit card number and date when making payments 

### Fixed
- Fixed a bug that caused failed fingerprint authentications to return to the previous view
instead of allowing to retry entering the fingerprint

## [0.20.6]

### Fixed
- Fixed a bug that caused vPOS implementations to count deposit prices twice

## [0.20.5]

### Changed
- Now loading svg assets instead of png

### Fixed
- Fixed a race condition when loading assets

## [0.20.4]

### Fixed
- Fixed a regression in credit card input view
- Updated okhttp to 3.14.7 to fix an issue with Android 11

## [0.20.3]

### Fixed
- Payment finished event for customer card payments
- Use Color.WHITE for EAN13 barcode text's when a translucent window background is set

## [0.20.2]

### Fixed
- Mark customer card payment as always online payment

## [0.20.1]

### Added
- Added support for 'customerCardPOS' payment method
- Added new views and events for customerCardPOS

### Changed
- Improved support for small devices

### Fixed
- Added night mode support for dynamic assets

## [0.20.0]

### Added
- Added support for dynamic asset downloading, to update Assets use project.getAssets().update()

### Changed
- Redesigned payment screens, now with retailer specific images and less clutter

## [0.19.5]

### Fixed
- Fixed a bug that caused SEPACardInputView to retain prefilled state

## [0.19.4]

### Fixed
- Added workaround when SDK is initialized after Activity creation

## [0.19.3]

### Added
- Added support for adding payment origins over terminal payments

### Changed
- When in a single project app, adding payment methods does not show the project anymore

## [0.19.2]

### Fixed
- CreditCardInputView now works with external app 3d secure authentications

## [0.19.1]

### Fixed
- Fixed a bug that CreditCardInputView did not load when embedded in react-native
- Fixed a bug that vPOS discounts got added to the total quantity in the ShoppingCart

## [0.19.0]

### Fixed 
- Fixed a bug that caused products that are reduced in price to be able to merge with the same
product that is not reduced in price, resulting in odd behaviour

## [0.18.7] + [0.19.0-alpha05]

### Fixed 
- Parsing of required customer cards now works without presence of accepted field

## [0.18.6] + [0.19.0-alpha04]

### Fixed 
- Fixed a bug that caused checkouts to stop polling 

## [0.19.0-alpha03]

### Fixed
- Handle PAYMENT_APPROVED in payment selection

## [0.19.0-alpha02]

### Changes
- Added RawReceipts callback

## [0.19.0-alpha01]

### Breaking Changes
- Updated ui callbacks, now using an interface with an enum instead of an interface with functions
- Splitting up of CheckoutView into multiple Views (PaymentSelectionView, CheckoutOnlineView,
  CheckoutOfflineView, CheckoutGatekeeperView and CheckoutPointOfSaleView)
 
### Fixed
- Various compatibility fixes for usage in react-native


## [0.18.5]

### Fixed
- Bundle products not showing up if they have a price > 0 

## [0.18.4]

### Fixed
- Added missing night mode icon

## [0.18.3]

### Changed 

- Added new gatekeeper icon
- Enabled credit card payments by default

## [0.18.2]

### Added
- Added setManualCameraControl to SelfScanningView to control the camera by the app itself

### Fixed
- Updating ShoppingCartView in onAttachedToWindow to fix an issue when reusing existing 
ShoppingCartView's and displaying them again instead of creating a new instance

## [0.18.1]

### Changes
- Expose client id setter

## [0.18.0]

### Changes
- Added Keyguard functionality to SDK itself
- Removed SnabbleUI.requestKeyguard callback

## [0.17.10]

### Fixed  
- Fixed a bug that prevented items from counting up after scanning
- Fixed a bug that caused payment limits to display multiple times while scanning
- Fixed a bug that caused wrong totals for discounts when using vPOS 

## [0.17.9]

### Fixed 
- SelfScanningFragment not recreating SelfScanningView when reattached using FragmentManager

## [0.17.8]

### Added
- Support vPOS sale stop
- Added event when payment credentials are not accessible anymore

### Changed
- Show empty prices in scanner and cart
- Adjusted vertical touch slop for cart swipe to refresh

## [0.17.7]

### Fixed
- Handle overwritten scannedCodes from vPOS

## [0.17.6]

### Added
- Added support for vPOS 

### Fixed
- Commission items are now not editable anymore

### New String Keys
- Snabble.Shoppingcart.discounts
- Snabble.Shoppingcart.giveaways

## [0.17.5]

### Added
- Simple filtering of payment methods

## [0.17.4]

### Fixed
- Fixed payment result not resetting on consecutive checkouts

## [0.17.3]

### Added
- Added support for terminal handover
- Added support for adding payment origins over terminal payments
- Added OnKeyListener to PaymentResolver

## [0.17.2]

### Changed
- Removed delay for checkout abort button appearance

### Fixed
- More night mode fixes

## [0.17.1]

### Fixed
- Fixed night mode icon for camera permission

## [0.17.0]

### Breaking Changes 
- Minimum API Level is now 21.
- Java 8 is now required.
- All ui components are now using the material components theme. 

It is now REQUIRED to use a material components theme for your app. AppCompat themes are not supported anymore.

For more detailed information see:
https://github.com/material-components/material-components-android/blob/master/docs/getting-started.md

### Added
- Night mode support, if a DayNight theme is used.
- Support for employee card payments

### Removed
- All snabble_* colors, all components are now following the material components color system.
Only the snabble_info* colors are left, but can mostly be left untouched.
 
## [0.16.10]

### Fixed
- Fixed payment selection ActionBar title

### Changed
- Send cart again after checkout abort 
- Increased toast message duration for longer texts
- Added 3d secure hint

## [0.16.9]

### Fixed
- Notify payment updates on main thread instead of caller thread

## [0.16.8]

### Added
- Added additional telemetry events

## [0.16.7]

### Added
- Encoded codes can now be sorted by using EncodedCodesOptions.sorter 

## [0.16.6]

### Changed 
- Await and block for abort calls while in checkout
- A short Checkout-ID is now visible while in payment 
- Hide bundles of products without a price
- Removed restriction to german IBAN's
- Filter payment methods by appId

### New String Keys
- Snabble.Payment.cancelError.title
- Snabble.Payment.cancelError.message

## [0.16.5]

### Fixed
- Removed unnecessary cart update on empty carts

## [0.16.4]

### Changed
- Info boxes are now showing longer/shorter based on text length
- Added snabble_textColorOnPrimary, for coloring elements that are on the primary color 

## [0.16.3]

### Fixed
- Improved behaviour of offline checkout retryer

## [0.16.2]

### Added
- Option to show the product sku in ProductSearchView

## [0.16.1]

### Changed
- Support finalCode for ikea code

## [0.16.0]

### Changed
- Replaced credit card icon

### New String Keys
- Snabble.Payment.CreditCard

## [0.16.0-beta3]

### Fixed 
- Do not show credit card payment options when not enabling support for it

## [0.16.0-beta2]

### Fixed
- Flicker when loading credit card input 

## [0.16.0-beta1]

### Breaking Changes
- Added showCreditCardInput() to SnabbleUICallback. If you don't use credit card payments, you
can leave the implementation empty.

### Added
- Experimental support for credit card payments. To enable set Config.enableExperimentalCreditCardPayment.
Only supported on API 21+.

- Checkouts are now persisted and will be transferred at a later time.
- Now always showing a payment method selection when entering a new payment method.

## [0.15.3]

### Added
- ProductNotFound events are now posted when a scanned product is not found 

### Changed
- ReceiptInfo.getDate() is now deprecated, use getTimestamp() instead.

## [0.15.2]

### Changes
- Increased shopping cart view auto text size
- Removed payment select empty state icon 

## [0.15.1]

### Fixed
- Fixed receipts pdf download

## [0.15.0]

### Breaking Changes
- Removed RECEIPT_AVAILABLE checkout state
- Changed ReceiptInfo.getProject to return a string instead of the resolved project
- Removed receipt ui components 

## [0.14.18]

### Fixed
- Fixed parsing of encodedCodes

## [0.14.17] 

### Fixed
- EncodedCodes CSV format headers

## [0.14.16]

### Fixed
- Properly handle unknown checkout info errors

## [0.14.15]

### Fixed
- Reuse session id on cart backup/restore
- Rare crash on BarcodeScannerView when resuming the camera
- Fixed text being cutoff on restore cart button

## [0.14.14]

### Added
- Support for new qrCodeOffline metadata

### Changed
- Removed support for old encodedCodes payment methods

## [0.14.13]

### Added
- Ability to restore the previous shopping cart, after checkout. 

### Fixed
- Fixed SEPA card input not closing after entry when activity got destroyed before resuming while accepting keyguard

### New String Keys
- Snabble.Shoppingcart.emptyState.restartButtonTitle 
- Snabble.Shoppingcart.emptyState.restoreButtonTitle

## [0.14.12]

### Fixed
- Crash on moto g7 plus when activating the torch

## [0.14.11]

### Changed
- Choose offline payment method based on available payment methods in metadata

## [0.14.10]

### Changed
- Added accessor to additional metadata
- Added available payment methods to project 

## [0.14.9]

### Changes
- Now showing the 'checkout done' screen when pressing 'done' on encoded codes screen
- Added snap scrolling to encoded codes screen
- Improved layout for medium sized devices on encoded codes screen

### Fixed
- Fixed a race condition in shopping cart product updates

## [0.14.8]

### Changes
- Removed sorting by price in encodedCodes

## [0.14.7]

### Changes
- Added support for encodedCodes 'maxChars' property
- Layout and Behaviour improvements to the encodedCodes screen
- Renamed key Snabble.Receipt.noReceipt to Snabble.Receipts.noReceipt 

### New String Keys
- Snabble.QRCode.entry.title
- Snabble.Receipts.noReceipt

## [0.14.6]

### Fixed
- Assume fractional unit is 0 when no conversion is possible of encoded unit
- Use scanned code when selecting bundle product

## [0.14.5]

### Changes 
- Allow shorter codes to be matched if the last code template group is a ignore group
- Embed lookup code instead of zero-filled scanned code when searching by code

## [0.14.4]

### Fixed 
- Fixed a bug that caused the checkout limit message to not appear when checking out im some cases

## [0.14.3]

### Changes
- Now exposing orderId in Checkout
- ReceiptListView is now checking for orderId while fetching
 
## [0.14.2]

### Fixed
- Fixed a crash when opening ReceiptListView without setting a Project first

## [0.14.1]

### Fixed
- Fixed a bug that caused transmissionCodes not to be applied in encodedCodes when not in the
default code template

## [0.14.0]

### Breaking Changes
- Checkout now has a RECEIPT_AVAILABLE state, which gets fired after PAYMENT_SUCCESSFUL when
the receipt is generated by the backend. If you relied on using ui components (using CheckoutView), 
this change is non-breaking.
- ShoppingCart.toBackendCart is now package-private

### Added
- Added the option to override the success and failure views in CheckoutView
- ReceiptListView is now showing when a Receipt is still in the process of being generated

### Changed
- Prefixed all resources with snabble, to avoid clashes with app resources

## [0.13.17]

### Added
- Added support for customer card prices
- Added support for scan messages

## [0.13.16]

### Added
- Project now contains getName() for a user presentable name

## [0.13.15]

### Changed
- The default scan area is now 20% bigger as the indicator indicates
- Now warming up the image cache when adding items to the shopping cart
- Added setRestrictionOvershoot to BarcodeScannerView to increase the detection area by a 
multiplier based on the indicator

### Fixed
- Fixed payment status screen not updating when in a stopped activity 
- Fixed scanner shopping cart button not updating when in a stopped activity
- Fixed barcode search crashing when multiple templates were matching on a single product with the same input

## [0.13.14]

### Added
- Added Config parameter maxShoppingCartAge

### Removed
- Removed config parameter enableReceiptAutoDownload as it is no longer used

### Changes
- Add keepScreenOn flag on checkout view

### Fixed
- Null pointer in very rare circumstances using ZXing Barcode Detector 
- CheckoutSuccessful Telemetry Event was not firing

## [0.13.13]

### Added
- Added support for constant code groups on code templates

### Changes
- Updated outdated product database mime types

## [0.13.12]

### Changed
- Improved SEPA mandate info dialog

### Fixed
- Now correctly escaping line feeds in product queries 
- Fixed SEPA mandate dialog not showing when credentials were not encrypted using a keyguard

## [0.13.11]

### Fixed
- Now tinting payment success image with snabble_primaryColor 
- Keyguard authentication on Android < 4.3

## [0.13.10]

### Fixed
- Prevent repeating of customer card ids in encoded codes

## [0.13.9]

### Fixed
- Fixed encoded codes generating multiple times per layout pass

## [0.13.8]

### Changed
- Payment status view now shows a qr code instead of a data matrix code 

## [0.13.7]

### Fixed
- Handle missing customer card id in encodedCodes

## [0.13.6]

### Added
- Support for maximum size of encoded code qr code display
- Support for additional payment methods

### Fixed
- Race condition when updating product database and shopping cart prices at the same time
- Fixed a bug with BarcodeView not properly fading in when outside bounds
- Reencode user input for zero amount products in backend items 

## [0.13.5]

### Added 
- Added new onCheckoutLimitReached and onOnlinePaymentLimitReached callbacks to ShoppingCartListener. 

This is a minor breaking change if you use the verbose listener because of additional interface methods.

- Checkout has now an additional state: NO_PAYMENT_METHOD_AVAILABLE

### Changed 
- Added limit messages to shopping cart in addition to limit warnings in self scanning.
- Added new message when no payment methods are available

### New string keys
- Snabble.Payment.noMethodAvailable

## [0.13.4]

### Fixed 
- Additional codes now also get prefixed with count in encoded codes csv generation

## [0.13.3]

### Changed
- Improved memory usage of encoded codes view

### Fixed 
- Scanner buttons tint color on API <= 19
- Cart events are now sending correctly after a successful checkout
- Encoding of zero amount products in encoded codes
- Searches in non-default templates are now using the template for generating the selected code

## [0.13.2]

### Fixed
- Fixed a crash when generating price overrides, due to an api incompatibility
- Fixed parsing of price override codes when no transmission template or code is set

## [0.13.1]

### Added
- ScanIndicator has now Rectangle and Quadratic styles, via BarcodeScannerView.setIndicatorStyle

### Changed
- Improved BarcodeScannerView ScanIndicator
- Removed support for 'teleCashDeDirectDebit' in favor of 'deDirectDebit' payment method

### Fixed
- Show error message when no payment method is available

## [0.13.0]

### Breaking Changes
- SnabbleUICallback now needs showShoppingCart() to be implemented 
- Using new ShoppingCart implementation, breaking core SDK API.
- Now using material components theme as the base for designing ui components, this may
result in odd behaviour when not using a material components theme as your base

### Added
- Enhanced security of locally stored payment credentials by using the Android KeyStore API
on Android 4.3+
- Support for automatic remote price updates and promotions

### Changed
- Lots and lots of small ui changes on almost every part in the ui components
- ShoppingCartView is now using insert/remove Animations
- PriceFormatter is now reused across projects
- Keep 4 starting digits of obfuscated iban 
- Now showing original price when discounted prices are set in product confirmation dialog
- Now showing sale stops directly after scanning
- Now showing a message if a checkout limit is reached

### Fixed
- Products with a overridden base price now get properly send over to the backend
- ShoppingCart checkout button visible when cart is empty 
- SearchableProductAdapter is now honoring searchable templates
- Keyguard is now correctly preventing checkout 

### New string keys
- Snabble.Scanner.goToCart
- Snabble.Scanner.goToCart.empty
- Snabble.saleStop.errorMsg.scan
- Snabble.limitsAlert.title 
- Snabble.limitsAlert.notAllMethodsAvailable
- Snabble.limitsAlert.checkoutNotAvailable
- Snabble.Payment.SEPA.hint
- Snabble.Keyguard.requireScreenLock

## [0.12.4]

### Fixed
- Fixed an issue that prevented repeated network calls would reuse dead connection
pools and result in network errors even when network was available again
- Subtitle visibility for recycled shopping cart cells

## [0.12.3]

### Fixed
- Added missing Checkout-ID to QRCodePOS checkout types
- Fixed hiding of explanation text on small displays (QVGA or smaller)
- Checkout screen with QRCodePOS type now also sets the ActionBar title if an ActionBar is
set with SnabbleUI.registerActionBar

## [0.12.2]

### Changed
- Improved layout of checkout status screen
- List dividers are now using the style attribute android:listDivider
- The color snabble_dividerColor is removed

## [0.12.1]

### Fixed
- Configured FirebaseBarcodeDetector to detect UPC-A as EAN13 to match the behaviour of ZXing

## [0.12.0]

### Fixed
- Top down info box appearing again on layout change
- Quantity is now not editable anymore over keyboard input for commission products

## [0.12.0-beta7]

### Fixed
- Restored payment credentials list empty state

## [0.12.0-beta6]

### Fixed
- Fixed border width to be the same on all bordered buttons 

## [0.12.0-beta5]

### Changes
- Show network error on receipts list if receipt list could not be fetched 
- Use FloatingActionButton for payment list view
- Unified ic_chevron and ic_chevron_right

### New string keys
- Snabble.networkError

## [0.12.0-beta4]

### Changes
- Add support for new receipts api
- OkHttpClient is now shared across projects

## [0.12.0-beta3]

### Important Changes
- The minimum required database schema version is now 1.18. Backwards support is dropped and 
opening a old database will result in deletion of the database and fallback to online 
only mode. Calling update() will download a up to date version of the database.

If you are using a bundled database make sure to update it to a database with schema 1.18 or higher.

### Breaking Changes
- ScannableCode is now renamed to ScannedCode
- Product.getScannableCodes() is now returning a Product.Code object

### Changes
- ScannedCode.parse is now returning a list of ScannedCode matches which need to be looked up 
in the database
- Support for Units is now migrated to new database schema, the older schema used in previous 
versions will fall back to g/kg only

- Added more Units and removed isXXX functions in favor of Unit.getDimension()
- Small layout improvements for qr and encoded codes checkout

### Added
- Support for code templates
- Support for product api v2

### Fixed
- Crash when a network error occurred when scanning a product

### New string keys
- Snabble.Shoppingcart.buyProducts.now

## [0.12.0-beta2]

### Added
- Added support for encoded codes csv 

## [0.12.0-beta1]

### Important Changes
- All requests to our domains are now using certificate pinning

### Changed
- Renamed get/set LoyaltyCardId to CustomerCardId 
- Now listing the offending products if a checkout is failing because of a sale stop or other 
various reasons

### Added
- Added support for Units (ml, kg, cm...)
- Added support for customer card metadata
- Added customizing options for ProductSearchView
- Added top-down info box in UIUtils
- Database error event logging (to the servers of snabble)
- Parsing of reference units 

### Fixed
- Directly showing keyboard when entering barcode when no database is available and the 
- Fixed dialog showing "null " + Product name when no subtitle is set
- Use reference units for weight/amount transmission when checking out

### New string keys
- Snabble.saleStop.errorMsg.title
- Snabble.saleStop.errorMsg.one
- Snabble.saleStop.errorMsg

## [0.11.2]

### Fixed
- Prices are now updated when updating the ProductDatabase
- Always show the locally calculated price for consistency

## [0.11.1]

### Fixed
- Force US locale for time formatting

## [0.11.0]

### Changes
- SnabbleUICallback has now additional interface methods that needs to be implemented, 
all payment related callbacks are optional if no online payment method is used.

showMainscreen() has been replaced with goBack() for more intuitive behaviour

For more information see the sample application

- Product dialog can now be shown anywhere, not only while having the scanner opened using
the new class ProductResolver

- Exposed OkHttpClient in Project, to allow for making Requests that use certificate pinning
and a valid token without extra effort, this also means OkHttp is now part of our public API

### Added
- Add support for online payment using SEPA
- Add support for zebra hardware scanner devices
- Add firebase barcode detector module
- Product and Shop are now Parcelable
- Added toShortString in Product and Shop
- Added config parameter maxProductDatabaseAge. Product database lookups are forced to
 make online requests when the time since the last update exceeds this value. The default is 1 hour.

### Fixed
- Fixed products with embeddedCode == 0 not showing "scanned shelf code" info
- Properly cleanup okhttp connections on error responses
- Log events in millisecond precision
- BarcodeScannerView: Immediately auto focus on startup 
- BarcodeScannerView: Unrecoverable errors (such as the camera driver not responding) 
should now show the error text instead of a black screen

### Removed
- ProductDatabase.getBoostedProducts and Product.getBoost

### New String keys
- Snabble.Payment.SEPA.Name
- Snabble.Payment.SEPA.IBAN
- Snabble.Payment.SEPA.InvalidIBAN
- Snabble.Payment.SEPA.InvalidName
- Snabble.Save
- Snabble.Payment.emptyState.message
- Snabble.Payment.emptyState.add
- Snabble.Payment.delete.message
- Snabble.Payment.add
- Snabble.Checkout.verifying
- Snabble.Checkout.done
- Snabble.Checkout.payAtCashRegister
- Snabble.Checkout.error

## [0.10.6]

### Fixed
- Fixed a crash on some Android 8 and 8.1 devices when updating the ProductDatabase

## [0.10.5]

### Fixed
- Show shop unspecific price, if no price is available for selected shop and database 
has pricingCategories set for shop

## [0.10.4]

### Fixed
- Endless recursion when scanning non EAN13 codes starting with 0 that will not result in a 
match on the database

## [0.10.3]

### Added
- Barcode false positive detection

## [0.10.2]

### Fixed
- Fixed checkout cancelling
- Product search item cells should now scale with multiline text 

## [0.10.1] 

### Added
- Experimental support for IKEA vendor specific codes

### Changed
- ITF code detection is now restricted to ITF14

## [0.10.0]

### Added
- Added experimental support for receipts

### Changed
- Add support for CODE_39
- Parse scanFormats from metadata
- Now sorting products in qr codes by price
- Now falling back to showing qr code when no connection could be made
- Now showing undo snackbar instead of dialog for removal of products using the quantity controls

### Fixed
- Socket timeouts now call the error callbacks in every case

### New String Keys
  
- Snabble.Receipt.errorDownload 
- Snabble.Receipt.pdfReaderUnavailable

## [0.9.4]

### Changed
- Improved auto focus speed (especially on budget devices with slow continuous video auto focus)
- Increased camera resolution, if the device supports it

## [0.9.3]

### Changed
- BarcodeFormat is now part of core
- Deprecated Checkout.setShop, use Project.setCheckedInShop instead 
- Improved payment selection layout

### Added
- Added support for pricing categories 
- Added support for ITF barcodes
- Added support for multiple sku requests 

### Fixed
- Clearing reference to Camera PreviewCallback when leaving BarcodeScannerView
- Improved recovery from corrupted database files

## [0.9.2]

To use this release ui components you need to also migrate to androidx. See [https://developer.android.com/topic/libraries/support-library/refactor](https://developer.android.com/topic/libraries/support-library/refactor)

### Changed
- Support for build-tools 3.2.0
- Migrated to androidx

## [0.9.1]

### Fixed
- Improved camera error handling

## [0.9.0]  
  
This are the most notable changes from 0.8.21, for all changes see previous release notes or commit history:  

### Changed
- Initialization of the SDK is now done using Snabble.getInstance().setup  
- Most of the functionality that was in SnabbleSdk before is now contained in Project  
- For migration you can replace most calls that were made to a SnabbleSdk instance to Project, everything that is not migrated to Project is contained in Snabble  
- Config requires a new appId and secret  
- Database bundling is now provided by using the loadDatabaseBundle function in ProductDatabase  
- SnabbleUI.registerSnabbleSdk is now SnabbleUI.useProject  

### Added
- Support for multiple projects using one SDK instance  
- Support for new authentication scheme  
- Support for new metadata format  
- New products are now at the top of the shopping cart  
- Added support for edeka product codes  
- Added support for transmission codes  
- Improved performance of code search on Android 4.x  
- Improved asking for camera permission when using SelfScanningFragment  
- Improved performance of BarcodeView  
- Improved dialog behavior for small and larger width devices  
- New info snackbar in scanner  
- Code lookups of EAN8 codes are now also looking up its EAN13 counterpart  
- Checkout finish/abort telemetry  
- Added the option to showing a hint when adding the first item to the shopping cart  

### Fixed
  
- Fixed BarcodeView on API 16  
- Pausing the barcode scanner when showing hints  
- Fixed various issues that causes the BarcodeScanner to freeze  
- Restored shopping cart button ripple effects  
- Fixed undo for zero amount products  
- Fixed version string sometimes null in user agent  
- Be more tolerant to unset project scenarios  
- Fixed translating transmission codes  
- Disable polling for offline payment methods  
- Sorting EAN8 hits on top in code search  

### New String Keys
  
- Snabble.OK  
- Snabble.Hints.title  
- Snabble.Hints.closedBags  
- Snabble.goToSettings  
- Snabble.askForPermission
