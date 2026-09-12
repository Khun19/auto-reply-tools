package com.autoreplytools.core.adapters

import android.content.pm.PackageManager

class GeminiAdapter(packageManager: PackageManager) : SemanticAiAdapter(packageManager) {
    override val supportedPackages = setOf(
        "com.google.android.apps.bard",
        "com.google.android.apps.gemini",
    )
    override val inputTerms = setOf("message", "ask", "prompt", "type")
    override val sendTerms = setOf("send", "submit")
    override val generatingTerms = setOf("stop generating", "generating", "cancel")
}