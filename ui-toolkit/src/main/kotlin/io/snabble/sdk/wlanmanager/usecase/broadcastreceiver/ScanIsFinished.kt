package io.snabble.sdk.wlanmanager.usecase.broadcastreceiver

interface ScanIsFinished {

    suspend operator fun invoke(): Boolean
}
