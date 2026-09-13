package com.autoreplytools.core.accessibility

data class AccessibilityActionPolicy(
    val maxAttempts: Int = 3,
    val retryDelayMs: Long = 250L,
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be greater than zero" }
        require(retryDelayMs >= 0L) { "retryDelayMs cannot be negative" }
    }
}

data class AccessibilityActionResult(
    val succeeded: Boolean,
    val attempts: Int,
    val failureReason: String? = null,
) {
    init {
        require(attempts >= 0) { "attempts cannot be negative" }
        require(succeeded || !failureReason.isNullOrBlank()) {
            "A failed action must include a failure reason"
        }
    }
}
