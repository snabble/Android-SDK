package io.snabble.sdk.config

@JvmInline
value class ProjectId(val id: String)

sealed interface CustomProperty

data object ExternalBillingSubjectLength : CustomProperty
