package io.snabble.sdk.sample

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationBarView
import io.snabble.sdk.SnabbleUiToolkit
import io.snabble.sdk.ui.SnabbleUI

fun setUpUiEvents(activity: AppCompatActivity, navController: NavController, bottomNavigationView: NavigationBarView) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_BARCODE_SEARCH
    ) { _, args ->
        navController.navigate(R.id.navigation_barcode_search, args)
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_SCANNER
    ) { _, args ->
        navController.navigate(R.id.navigation_scanner, args)
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_SHOPPING_CART
    ) { _, _ ->
        bottomNavigationView.selectedItemId = R.id.navigation_scanner
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_SEPA_CARD_INPUT
    ) { _, args ->
        navController.navigate(R.id.navigation_sepa_card_input, args)
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_CREDIT_CARD_INPUT
    ) { _, args ->
        navController.navigate(R.id.navigation_credit_card_input, args)
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_PAYDIREKT_INPUT
    ) { _, args ->
        navController.navigate(R.id.navigation_paydirekt_input, args)
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_PAYONE_INPUT
    ) { _, args ->
        navController.navigate(R.id.navigation_payone_input, args)
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.SHOW_AGE_VERIFICATION
    ) { _, args ->
        navController.navigate(R.id.navigation_age_verification, args)
    }
    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.NOT_CHECKED_IN
    ) { _, args ->
        navController.navigate(R.id.not_checked_in, args)
    }

    SnabbleUI.setUiAction(
        activity,
        SnabbleUI.Event.GO_BACK
    ) { _, _ ->
        navController.popBackStack()
    }

    SnabbleUiToolkit.setUiAction(
        activity,
        SnabbleUiToolkit.Event.SHOW_SHOP_LIST
    ) { _, args ->
        bottomNavigationView.selectedItemId = R.id.navigation_shop
    }

    SnabbleUiToolkit.setUiAction(
        activity,
        SnabbleUiToolkit.Event.SHOW_DETAILS_SHOP_LIST
    ) { _, args ->
        navController.navigate(R.id.navigation_shops_details, args)
    }
    SnabbleUiToolkit.setUiAction(
        activity,
        SnabbleUiToolkit.Event.DETAILS_SHOP_BUTTON_ACTION
    ) { _, _ ->
        bottomNavigationView.selectedItemId = R.id.navigation_scanner
    }
    SnabbleUiToolkit.setUiAction(
        activity,
        SnabbleUiToolkit.Event.SHOW_DEEPLINK
    ) { _, args ->
        val uri = Uri.parse(requireNotNull(args?.getString(SnabbleUiToolkit.DEEPLINK)))
        navController.navigate(NavDeepLinkRequest.Builder.fromUri(uri).build())
    }
    SnabbleUiToolkit.setUiAction(
        activity,
        SnabbleUiToolkit.Event.SHOW_ONBOARDING
    ) { _, args ->
        navController.navigate(R.id.frag_onboarding, args)
    }
    SnabbleUiToolkit.setUiAction(
        activity,
        SnabbleUiToolkit.Event.SHOW_ONBOARDING_DONE
    ) { _, _ ->
        navController.popBackStack()
        if (!sharedPreferences.contains(MainActivity.PREF_KEY_SHOW_ONBOARDING)) {
            sharedPreferences.edit()
                .putBoolean(MainActivity.PREF_KEY_SHOW_ONBOARDING, false)
                .apply()
        }
    }
}
