package com.autoreplytools.core.adapters

import android.content.pm.PackageManager

class ChatGptAdapter(packageManager: PackageManager) : SemanticAiAdapter(packageManager) {
    override val supportedPackages = setOf("com.openai.chatgpt")
    override val inputTerms = setOf("message", "ask", "prompt", "type")
    override val sendTerms = setOf("send", "submit")
    override val generatingTerms = setOf("stop generating", "generating", "cancel")
}