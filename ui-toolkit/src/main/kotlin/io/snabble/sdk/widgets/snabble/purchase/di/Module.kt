package io.snabble.sdk.widgets.snabble.purchase.di

import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatter
import io.snabble.sdk.widgets.snabble.purchase.RelativeTimeStringFormatterImpl
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepository
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepositoryImpl
import io.snabble.sdk.widgets.snabble.purchase.usecases.GetPurchasesUseCase
import io.snabble.sdk.widgets.snabble.purchase.usecases.GetPurchasesUseCaseImpl
import io.snabble.sdk.widgets.snabble.purchase.viewmodel.PurchaseViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val purchaseWidgetModule = module {
    viewModelOf(::PurchaseViewModel)

    factoryOf(::GetPurchasesUseCaseImpl) bind GetPurchasesUseCase::class

    factory {
        PurchasesRepositoryImpl(
            snabble = get(),
            timeFormatter = get(),
        )
    } bind PurchasesRepository::class

    factoryOf(::RelativeTimeStringFormatterImpl) bind RelativeTimeStringFormatter::class
}
