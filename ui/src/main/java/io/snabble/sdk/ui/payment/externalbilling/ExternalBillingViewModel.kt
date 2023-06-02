package io.snabble.sdk.ui.payment.externalbilling

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.snabble.sdk.Snabble
import io.snabble.sdk.ui.payment.externalbilling.domain.ExternalBillingRepositoryImpl
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ExternalBillingViewModel : ViewModel() {

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val project = Snabble.checkedInProject.value
            project?.let {
                val repo = ExternalBillingRepositoryImpl(
                    it,
                    Json { ignoreUnknownKeys = true }
                )
                val result = repo.login(username, password)
                Log.d("xx", "login: $result")
            }
        }
    }
}
