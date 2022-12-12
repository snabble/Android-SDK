package io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase

import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepository
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepositoryImpl

internal interface DenyPayoneSepaMandateUseCase {

    operator fun invoke()
}

internal class DenyPayoneSepaMandateUseCaseImpl(
    private val repo: PayoneSepaMandateRepository = PayoneSepaMandateRepositoryImpl(snabble = Snabble),
) : DenyPayoneSepaMandateUseCase {

    override fun invoke() = repo.denyMandate()
}
