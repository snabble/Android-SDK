# Changelog  
All notable changes to this project will be documented in this file.

## [0.11.0]

### Changes
- SnabbleUICallback has now additional interface methods that needs to be implemented, 
all payment related callbacks are optional if no online payment method is used.

For more information see the sample application

- Product dialog can now be shown anywhere, not only while having the scanner opened using
the new class ProductResolver

### Added
- Add support for online payment using SEPA
- Add support for zebra hardware scanner devices
- Add firebase barcode detector module
- Product and Shop are now Parcelable
- Added toShortString in Product and Shop
- Added config parameter maxProductDatabaseAge. Product database lookups are forced to
 make online requests when the time since the last update exceeds this value. The default is 1 hour.

### Fixed
- Properly cleanup okhttp connections on error responses
- Log events in millisecond precision
- BarcodeScannerView: Immediately auto focus on startup 

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
