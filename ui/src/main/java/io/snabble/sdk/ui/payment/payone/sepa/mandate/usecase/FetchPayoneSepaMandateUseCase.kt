package io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase

import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepository
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepositoryImpl

internal interface FetchPayoneSepaMandateUseCase {

    operator fun invoke(): String?
}

internal class FetchPayoneSepaMandateUseCaseImpl(
    private val repo: PayoneSepaMandateRepository = PayoneSepaMandateRepositoryImpl(snabble = Snabble),
) : FetchPayoneSepaMandateUseCase {

    override fun invoke(): String? = repo.createSepaMandateHtml()
}
