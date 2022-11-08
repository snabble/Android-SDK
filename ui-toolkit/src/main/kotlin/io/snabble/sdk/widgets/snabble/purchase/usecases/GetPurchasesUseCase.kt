package io.snabble.sdk.widgets.snabble.purchase.usecases

import io.snabble.sdk.widgets.snabble.purchase.Purchase
import io.snabble.sdk.widgets.snabble.purchase.repository.PurchasesRepository

internal interface GetPurchasesUseCase {

    suspend operator fun invoke(count: Int): List<Purchase>
}

internal class GetPurchasesUseCaseImpl(
    private val repo: PurchasesRepository,
) : GetPurchasesUseCase {

    override suspend fun invoke(count: Int): List<Purchase> = repo.getPurchases(count)
}
