package io.snabble.sdk.customization.repository

import android.content.res.AssetManager
import com.google.gson.Gson
import io.snabble.sdk.screens.onboarding.data.OnboardingModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface OnboardingRepository {

    suspend fun getOnboardingModel(): OnboardingModel?
}

class OnboardingRepositoryImpl(
    private val assetManager: AssetManager,
    private val gson: Gson,
) : OnboardingRepository {

    override suspend fun getOnboardingModel(): OnboardingModel? = withContext(Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        val serializedConfig = assetManager.open(CONFIG_FILE_NAME).bufferedReader().readText()
        gson.fromJson(serializedConfig, OnboardingModel::class.java)
    }

    companion object {

        private const val CONFIG_FILE_NAME = "onboardingConfig.json"
    }
}
