package io.snabble.sdk.di

import android.content.Context
import androidx.startup.Initializer
import io.snabble.sdk.config.di.configModule
import io.snabble.sdk.domain.di.domainModule
import io.snabble.sdk.ui.toolkit.BuildConfig
import io.snabble.sdk.ui.widgets.customercard.di.customerCardModule
import io.snabble.sdk.ui.widgets.locationpermission.di.locationPermissionModule
import io.snabble.sdk.ui.widgets.purchase.di.purchaseWidgetModule
import io.snabble.sdk.ui.widgets.stores.di.storesModule
import io.snabble.sdk.ui.widgets.toggle.di.toggleWidgetModule
import io.snabble.sdk.ui.widgets.wifi.di.wifiModule
import io.snabble.sdk.usecases.di.useCaseModule
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
                domainModule,
                locationPermissionModule,
                purchaseWidgetModule,
                storesModule,
                toggleWidgetModule,
                useCaseModule,
                wifiModule,
            )
        }
            .koin
            .also(KoinProvider::setKoin)

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
