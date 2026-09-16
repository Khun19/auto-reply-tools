package com.autoreplytools.core.automation

import android.content.Context
import com.autoreplytools.core.accessibility.AccessibilityController
import com.autoreplytools.core.adapters.AiAppAdapter
import com.autoreplytools.core.adapters.ChatGptAdapter
import com.autoreplytools.core.adapters.GeminiAdapter
import com.autoreplytools.core.adapters.ViberAdapter
import com.autoreplytools.core.logging.AppLogger
import com.autoreplytools.core.model.AiProvider
import com.autoreplytools.core.model.AutomationState
import com.autoreplytools.core.model.AutomationTask
import com.autoreplytools.core.model.AutomationTimeouts
import com.autoreplytools.storage.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AutomationEngine(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val duplicateGuard: DuplicateGuard,
    private val whitelist: Whitelist,
    private val controllerProvider: () -> AccessibilityController?,
    private val logger: AppLogger,
    private val timeouts: AutomationTimeouts = AutomationTimeouts(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val queue = Channel<AutomationTask>(
        capacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableState = MutableStateFlow(AutomationState.IDLE)
    val state: StateFlow<AutomationState> = mutableState.asStateFlow()
    private var worker: Job? = null
    private var stopped = false

    fun start() {
        if (worker?.isActive == true) return
        worker = scope.launch {
            for (task in queue) process(task)
        }
    }

    suspend fun enqueue(task: AutomationTask): Boolean {
        if (stopped) return false
        val settings = settingsRepository.settings.first()
        if (!settings.enabled || !whitelist.isAllowed(task.sender, settings.whitelist)) return false
        if (!duplicateGuard.shouldProcess(task.sender, task.originalMessage, task.timestamp)) return false
        return queue.trySend(task).isSuccess
    }

    fun emergencyStop() {
        stopped = true
        while (queue.tryReceive().isSuccess) Unit
        mutableState.value = AutomationState.STOPPED
    }

    fun resume() {
        stopped = false
        mutableState.value = AutomationState.IDLE
        start()
    }

    private suspend fun process(task: AutomationTask) {
        val controller = controllerProvider()
        if (controller == null) {
            fail("Accessibility service is not enabled")
            return
        }

        val settings = settingsRepository.settings.first()
        val aiAdapter = createAiAdapter(task.targetAiProvider)
        val viberAdapter = ViberAdapter()
        val overallSuccess = withTimeoutOrNull(timeouts.overallMs) {
            transition(AutomationState.VIBER_MSG_DETECTED)

            transition(AutomationState.OPEN_AI_APP)
            if (!requireAction(timeouts.openAppMs) { aiAdapter.openApp(context) }) {
                return@withTimeoutOrNull false
            }

            val beforeRoot = controller.awaitRoot(aiAdapter.supportedPackages, timeouts.openAppMs)
                ?: return@withTimeoutOrNull false
            val beforeSnapshot = com.autoreplytools.core.text.TextExtractor().collect(beforeRoot)

            transition(AutomationState.PASTE_TO_AI)
            val aiTextResult = controller.setTextWithRetry(
                packageNames = aiAdapter.supportedPackages,
                finder = aiAdapter::findInput,
                text = task.originalMessage,
                timeoutMs = timeouts.pasteMs,
            )
            if (!aiTextResult.succeeded) {
                logger.warn(aiTextResult.failureReason ?: "AI text insertion failed")
                return@withTimeoutOrNull false
            }

            transition(AutomationState.SEND_TO_AI)
            val aiClickResult = controller.clickWithRetry(
                packageNames = aiAdapter.supportedPackages,
                finder = aiAdapter::findSend,
                timeoutMs = timeouts.sendMs,
            )
            if (!aiClickResult.succeeded) {
                logger.warn(aiClickResult.failureReason ?: "AI send action failed")
                return@withTimeoutOrNull false
            }

            transition(AutomationState.WAIT_AI_RESPONSE)
            val response = awaitResponse(controller, aiAdapter, beforeSnapshot)
                ?: return@withTimeoutOrNull false

            transition(AutomationState.COPY_AI_RESPONSE)
            if (response.isBlank()) return@withTimeoutOrNull false

            transition(AutomationState.OPEN_VIBER)
            val conversationIntent = task.conversationIntent?.let {
                try {
                    it.send()
                    true
                } catch (_: Exception) {
                    false
                }
            } ?: false
            if (!conversationIntent && !viberAdapter.openApp(context)) return@withTimeoutOrNull false

            controller.awaitRoot(viberAdapter.supportedPackages, timeouts.openAppMs)
                ?: return@withTimeoutOrNull false
            transition(AutomationState.PASTE_TO_VIBER)
            val viberTextResult = controller.setTextWithRetry(
                packageNames = viberAdapter.supportedPackages,
                finder = viberAdapter::findInput,
                text = response,
                timeoutMs = timeouts.pasteMs,
            )
            if (!viberTextResult.succeeded) {
                logger.warn(viberTextResult.failureReason ?: "Viber text insertion failed")
                return@withTimeoutOrNull false
            }

            transition(AutomationState.SEND_TO_VIBER)
            val viberClickResult = controller.clickWithRetry(
                packageNames = viberAdapter.supportedPackages,
                finder = viberAdapter::findSend,
                timeoutMs = timeouts.sendMs,
            )
            if (!viberClickResult.succeeded) {
                logger.warn(viberClickResult.failureReason ?: "Viber send action failed")
                return@withTimeoutOrNull false
            }

            transition(AutomationState.VERIFY)
            val verified = verifySend(controller, viberAdapter, response)
            if (!verified) return@withTimeoutOrNull false

            transition(AutomationState.IDLE)
            true
        } ?: false

        if (overallSuccess != true) fail("Automation run failed safely for task ${task.taskId}")
    }

    private suspend fun awaitResponse(
        controller: AccessibilityController,
        adapter: AiAppAdapter,
        before: com.autoreplytools.core.text.TextSnapshot,
    ): String? {
        var previous: String? = null
        var stableCount = 0
        return withTimeoutOrNull(timeouts.waitResponseMs) {
            var result: String? = null
            while (result == null) {
                val root = controller.awaitRoot(adapter.supportedPackages, 2_000L)
                if (root == null) continue
                if (adapter.isGenerating(root)) {
                    stableCount = 0
                    continue
                }
                val candidate = adapter.extractLatestResponse(before, root) ?: continue
                if (candidate == previous) stableCount++ else stableCount = 0
                previous = candidate
                if (stableCount >= 2) result = candidate
            }
            result
        }
    }

    private suspend fun verifySend(
        controller: AccessibilityController,
        viberAdapter: ViberAdapter,
        response: String,
    ): Boolean {
        val node = controller.awaitNode(viberAdapter.supportedPackages, { root ->
            val texts = com.autoreplytools.core.text.TextExtractor().collect(root).texts
            texts.firstOrNull { it.contains(response.take(24), ignoreCase = false) }?.let { root }
        }, timeouts.verifyMs)
        return node != null
    }

    private suspend fun requireAction(timeoutMs: Long, action: () -> Boolean): Boolean =
        withTimeoutOrNull(timeoutMs) { action() } == true

    private fun createAiAdapter(provider: AiProvider): AiAppAdapter =
        when (provider) {
            AiProvider.CHATGPT -> ChatGptAdapter(context.packageManager)
            AiProvider.GEMINI -> GeminiAdapter(context.packageManager)
        }

    private fun transition(next: AutomationState) {
        val current = mutableState.value
        val allowed = when (current) {
            AutomationState.IDLE -> setOf(AutomationState.VIBER_MSG_DETECTED, AutomationState.STOPPED)
            AutomationState.VIBER_MSG_DETECTED -> setOf(AutomationState.OPEN_AI_APP, AutomationState.ERROR)
            AutomationState.OPEN_AI_APP -> setOf(AutomationState.PASTE_TO_AI, AutomationState.ERROR)
            AutomationState.PASTE_TO_AI -> setOf(AutomationState.SEND_TO_AI, AutomationState.ERROR)
            AutomationState.SEND_TO_AI -> setOf(AutomationState.WAIT_AI_RESPONSE, AutomationState.ERROR)
            AutomationState.WAIT_AI_RESPONSE -> setOf(AutomationState.COPY_AI_RESPONSE, AutomationState.ERROR)
            AutomationState.COPY_AI_RESPONSE -> setOf(AutomationState.OPEN_VIBER, AutomationState.ERROR)
            AutomationState.OPEN_VIBER -> setOf(AutomationState.PASTE_TO_VIBER, AutomationState.ERROR)
            AutomationState.PASTE_TO_VIBER -> setOf(AutomationState.SEND_TO_VIBER, AutomationState.ERROR)
            AutomationState.SEND_TO_VIBER -> setOf(AutomationState.VERIFY, AutomationState.ERROR)
            AutomationState.VERIFY -> setOf(AutomationState.IDLE, AutomationState.ERROR)
            AutomationState.ERROR -> setOf(AutomationState.IDLE, AutomationState.STOPPED)
            AutomationState.STOPPED -> setOf(AutomationState.IDLE)
        }
        check(next == AutomationState.ERROR || next in allowed) {
            "Invalid automation transition: $current -> $next"
        }
        mutableState.value = next
    }

    private fun fail(reason: String) {
        logger.error(reason)
        mutableState.value = AutomationState.ERROR
        mutableState.value = AutomationState.IDLE
    }
}
