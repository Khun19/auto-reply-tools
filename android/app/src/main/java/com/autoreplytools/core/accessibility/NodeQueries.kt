package com.autoreplytools.core.accessibility

data class NodeQuery(
    val requiredPackage: String? = null,
    val editable: Boolean? = null,
    val clickable: Boolean? = null,
    val visible: Boolean = true,
    val enabled: Boolean? = null,
    val semanticTerms: Set<String> = emptySet(),
    val classNames: Set<String> = emptySet(),
    val minimumScore: Int = 1,
    val maxNodes: Int = 1_000,
) {
    init {
        require(minimumScore >= 0) { "minimumScore cannot be negative" }
        require(maxNodes > 0) { "maxNodes must be greater than zero" }
    }
}