package io.snabble.sdk.screens.receipts.usecase

import io.snabble.sdk.ReceiptInfo
import io.snabble.sdk.screens.receipts.domain.ReceiptsInfoRepository
import io.snabble.sdk.screens.receipts.domain.ReceiptsInfoRepositoryImpl

interface GetReceiptsInfoUseCase {

    suspend operator fun invoke(receiptId: String): ReceiptInfo?
}

internal class GetReceiptsInfoUseCaseImpl(
    private val receiptsInfoRepository: ReceiptsInfoRepository = ReceiptsInfoRepositoryImpl(),
) : GetReceiptsInfoUseCase {

    override suspend fun invoke(receiptId: String): ReceiptInfo? =
        receiptsInfoRepository.getReceiptsInfo(receiptId)
}
