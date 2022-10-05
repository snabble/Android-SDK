package io.snabble.sdk.ui.widgets.purchase.di

import io.snabble.sdk.ui.widgets.purchase.RelativeTimeStringFormatter
import io.snabble.sdk.ui.widgets.purchase.RelativeTimeStringFormatterImpl
import io.snabble.sdk.ui.widgets.purchase.repository.PurchasesRepository
import io.snabble.sdk.ui.widgets.purchase.repository.PurchasesRepositoryImpl
import io.snabble.sdk.ui.widgets.purchase.viewmodel.PurchaseViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val purchaseWidgetModule = module {
    factoryOf(::PurchasesRepositoryImpl) bind PurchasesRepository::class
    factoryOf(::RelativeTimeStringFormatterImpl) bind RelativeTimeStringFormatter::class
}
