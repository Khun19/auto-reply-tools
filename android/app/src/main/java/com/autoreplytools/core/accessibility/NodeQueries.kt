package com.autoreplytools.core.accessibility

data class NodeQuery(
    val requiredPackage: String? = null,
    val editable: Boolean? = null,
    val clickable: Boolean? = null,
    val visible: Boolean = true,
    val enabled: Boolean? = null,
    val semanticTerms: Set<String> = emptySet(),
    val classNames: Set<String> = emptySet(),
)