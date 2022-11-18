package io.snabble.sdk.di

import android.content.Context
import androidx.startup.Initializer
import io.snabble.sdk.dynamicview.data.di.configModule
import io.snabble.sdk.screens.home.di.homeModule
import io.snabble.sdk.screens.profile.di.profileModule
import io.snabble.sdk.ui.toolkit.BuildConfig
import io.snabble.sdk.widgets.snabble.customercard.di.customerCardModule
import io.snabble.sdk.widgets.snabble.locationpermission.di.locationPermissionModule
import io.snabble.sdk.widgets.snabble.purchase.di.purchaseWidgetModule
import io.snabble.sdk.widgets.snabble.stores.di.storesModule
import io.snabble.sdk.widgets.snabble.toggle.di.toggleWidgetModule
import io.snabble.sdk.widgets.snabble.wlan.di.wlanModule
import io.snabble.sdk.wlanmanager.di.wlanManagerModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.core.logger.Level
import org.koin.dsl.koinApplication

@Suppress("unused") // Usage in AndroidManifest.xml
internal class KoinInitializer : Initializer<Koin> {

    override fun create(context: Context): Koin =
        koinApplication {
            if (BuildConfig.DEBUG) printLogger(level = Level.DEBUG)
            androidContext(context)
            modules(
                configModule,
                coreModule,
                customerCardModule,
                locationPermissionModule,
                purchaseWidgetModule,
                storesModule,
                toggleWidgetModule,
                wlanModule,
                homeModule,
                profileModule,
                wlanManagerModule,
            )
        }
            .koin
            .also(KoinProvider::setKoin)

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
