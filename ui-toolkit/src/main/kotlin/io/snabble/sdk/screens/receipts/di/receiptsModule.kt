package io.snabble.sdk.screens.receipts.di

import io.snabble.sdk.screens.receipts.ReceiptProvider
import io.snabble.sdk.screens.receipts.domain.ReceiptsInfoRepository
import io.snabble.sdk.screens.receipts.domain.ReceiptsInfoRepositoryImpl
import io.snabble.sdk.screens.receipts.domain.ReceiptsRepository
import io.snabble.sdk.screens.receipts.domain.ReceiptsRepositoryImpl
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsInfoUseCase
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsInfoUseCaseImpl
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsUseCase
import io.snabble.sdk.screens.receipts.usecase.GetReceiptsUseCaseImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val receiptModule = module {
    factoryOf(::ReceiptsInfoRepositoryImpl) bind ReceiptsInfoRepository::class
    factoryOf(::ReceiptsRepositoryImpl) bind ReceiptsRepository::class

    factoryOf(::GetReceiptsInfoUseCaseImpl) bind GetReceiptsInfoUseCase::class
    factoryOf(::GetReceiptsUseCaseImpl) bind GetReceiptsUseCase::class

    factoryOf(::ReceiptProvider)
}
