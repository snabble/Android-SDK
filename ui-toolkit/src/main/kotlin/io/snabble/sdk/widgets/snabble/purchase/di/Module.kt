package io.snabble.sdk.widgets.snabble.purchase.di

import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatterImpl
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepositoryImpl
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatter
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val purchaseWidgetModule = module {
    factoryOf(::PurchasesRepositoryImpl) bind PurchasesRepository::class
    factoryOf(::RelativeTimeStringFormatterImpl) bind RelativeTimeStringFormatter::class
}
