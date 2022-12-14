package io.snabble.sdk.ui.payment.payone.sepa.mandate.usecase

import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepository
import io.snabble.sdk.ui.payment.payone.sepa.mandate.repositories.PayoneSepaMandateRepositoryImpl

internal interface AbortPayoneSepaMandateProcessUseCase {

    operator fun invoke()
}

internal class AbortPayoneSepaMandateProcessUseCaseImpl(
    private val repo: PayoneSepaMandateRepository = PayoneSepaMandateRepositoryImpl(snabble = Snabble),
) : AbortPayoneSepaMandateProcessUseCase {

    override fun invoke() = repo.abortProcess()
}
