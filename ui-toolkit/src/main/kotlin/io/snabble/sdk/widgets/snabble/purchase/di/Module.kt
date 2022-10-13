package io.snabble.sdk.widgets.snabble.purchase.di

import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatter
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatterImpl
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepository
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepositoryImpl
import io.snabble.sdk.widgets.snabble.purchase.viewmodel.PurchaseViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val purchaseWidgetModule = module {
    viewModelOf(::PurchaseViewModel)
    factoryOf(::PurchasesRepositoryImpl) bind PurchasesRepository::class
    factoryOf(::RelativeTimeStringFormatterImpl) bind RelativeTimeStringFormatter::class
}
