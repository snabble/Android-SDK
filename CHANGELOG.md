# Changelog  
All notable changes to this project will be documented in this file.

## [0.13.6]

### Added
- Support for maximum size of encoded code qr code display

### Fixed
- Race condition when updating product database and shopping cart prices at the same time

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
