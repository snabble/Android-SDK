package io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase

import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepository
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepositoryImpl

internal interface AcceptPayoneSepaMandateUseCase {

    suspend operator fun invoke(): Boolean
}

internal class AcceptPayoneSepaMandateUseCaseImpl(
    private val repo: PayoneSepaMandateRepository = PayoneSepaMandateRepositoryImpl(snabble = Snabble),
) : AcceptPayoneSepaMandateUseCase {

    override suspend fun invoke(): Boolean = repo.acceptMandate()
}
