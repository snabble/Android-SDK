package io.snabble.sdk.screens.receipts.usecase

import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.screens.receipts.domain.ReceiptsRepository
import io.snabble.sdk.screens.receipts.domain.ReceiptsRepositoryImpl
import java.io.File

interface GetReceiptsUseCase {

    suspend operator fun invoke(receiptInfo: ReceiptInfo): File?
}

internal class GetReceiptsUseCaseImpl(
    private val receiptsRepository: ReceiptsRepository = ReceiptsRepositoryImpl(),
) : GetReceiptsUseCase {

    override suspend fun invoke(receiptInfo: ReceiptInfo): File? =
        receiptsRepository.getReceipts(receiptInfo)
}
