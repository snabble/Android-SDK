# UI-API

## Requirements

- AndroidX
- Material components with a Material3 theme

## Usage

After initialisation of the core SDK, you can use `SnabbleUI.setProject` to register a
project with all UI components.

After that you should be able to either start an Activity like `SelfScanningActivity`
or use the various Fragments or View's.

For a more seamless integration it's recommended to use Fragments and include those in your
usual app flow.

Activities are less flexible, but work out of the box.

If you use a navigation framework that does not support Fragments, you can use the View's directly. 
But you need to make sure the Arguments in the provided Bundle are getting passed correctly. See the
Fragments source code for more details

Most screens require that a project is set, or they will crash. So make sure when you are recovering from 
state loss, that the SDK is initialized and the project is set again, before you display the view again.

Exceptions are View's for entering payment credentials.

## Inject into Navigation Flow

The SDK does need to switch to different Screens at different times (For example, when starting a Checkout).

If you do not register any UI actions, activities will be launched. If you want to integrate the SDK more deeply into 
your App, you need to add Handlers to Events.

Todo that, call `SnabbleUI.setUiAction` which will then be called instead of opening an Activity.

In some cases Activities will still be launched, which will then not invoke the UI action handler.

## Styling

The SDK uses Material 3 theme styling where possible. 

Activities can also use a MaterialToolbar, to enable using a Toolbar set the Theme attribute 
**snabbleToolbarStyle**.

## Night mode

The SDK supports Material 3 DayNight themes.

## List of UI Actions

- SHOW_CHECKOUT
- SHOW_CHECKOUT_DONE
- SHOW_SCANNER
- SHOW_BARCODE_SEARCH
- SHOW_SEPA_CARD_INPUT
- SHOW_CREDIT_CARD_INPUT
- SHOW_PAYONE_INPUT
- SHOW_PAYDIREKT_INPUT
- SHOW_SHOPPING_CART
- SHOW_PAYMENT_CREDENTIALS_LIST
- SHOW_PAYMENT_OPTIONS
- SHOW_PROJECT_PAYMENT_OPTIONS
- SHOW_AGE_VERIFICATION
- GO_BACK
- EVENT_PRODUCT_CONFIRMATION_SHOW
- EVENT_PRODUCT_CONFIRMATION_HIDE
- EVENT_EXIT_TOKEN_AVAILABLE

## List of UI Components

### Activities

- SelfScanningActivity
- CheckoutActivity
- ShoppingCartActivity
- ProductSearchActivity
- SEPACardInputActivity
- CreditCardInputActivity
- PayoneInputActivity
- PaydirektInputActivity
- PaymentOptionsActivity
- ProjectPaymentOptionsActivity
- PaymentCredentialsListActivity
- AgeVerificationActivity

### Fragments

- SelfScanningFragment
- CheckoutFragment
- ShoppingCartFragment
- ProductSearchFragment
- SEPACardInputFragment
- CreditCardInputFragment
- PayoneInputFragment
- PaydirektInputFragment
- PaymentOptionsFragment
- ProjectPaymentOptionsFragment
- PaymentCredentialsListFragment
- AgeVerificationFragment

### Views

- SelfScanningView
- CheckoutView
- ShoppingCartView
- ProductSearchView
- SEPACardInputView
- CreditCardInputView
- PayoneInputView
- PaydirektInputView
- PaymentOptionsView
- ProjectPaymentOptionsView
- PaymentCredentialsListView
- AgeVerificationView