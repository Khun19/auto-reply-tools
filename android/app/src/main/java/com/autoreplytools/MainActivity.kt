package com.autoreplytools

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autoreplytools.core.RuntimeContainer
import com.autoreplytools.core.model.AiProvider
import com.autoreplytools.core.model.AutomationState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var enableSwitch: Switch
    private lateinit var providerSpinner: Spinner
    private var providerInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RuntimeContainer.initialize(applicationContext)
        setContentView(buildScreen())
        observeState()
    }

    private fun buildScreen(): ScrollView {
        val padding = (24 * resources.displayMetrics.density).toInt()
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }
        val title = TextView(this).apply {
            text = getString(com.autoreplytools.R.string.app_name)
            textSize = 28f
            setTextColor(Color.rgb(20, 50, 45))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        statusText = TextView(this).apply {
            text = "Status: IDLE"
            textSize = 16f
            setPadding(0, padding / 2, 0, padding / 2)
        }
        enableSwitch = Switch(this).apply {
            text = "Enable automatic replies"
            setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch { RuntimeContainer.settingsRepository.setEnabled(checked) }
            }
        }
        val providerLabel = TextView(this).apply {
            text = "AI Provider"
            textSize = 16f
        }
        providerSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                AiProvider.values().map { it.displayName },
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long,
                ) {
                    if (!providerInitialized) return
                    val provider = AiProvider.values()[position]
                    lifecycleScope.launch { RuntimeContainer.settingsRepository.setAiProvider(provider) }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        val senderInput = EditText(this).apply {
            hint = "Allowed Viber sender"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }
        val addSender = Button(this).apply {
            text = "Add sender to whitelist"
            setOnClickListener {
                lifecycleScope.launch {
                    RuntimeContainer.settingsRepository.addWhitelistSender(senderInput.text.toString())
                    senderInput.text.clear()
                }
            }
        }
        val accessibility = Button(this).apply {
            text = "Open Accessibility settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        val notifications = Button(this).apply {
            text = "Open Notification access settings"
            setOnClickListener { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }
        }
        val stop = Button(this).apply {
            text = "Emergency STOP"
            setOnClickListener { RuntimeContainer.engine.emergencyStop() }
        }
        val resume = Button(this).apply {
            text = "Resume automation"
            setOnClickListener { RuntimeContainer.engine.resume() }
        }
        listOf(title, statusText, enableSwitch, providerLabel, providerSpinner, senderInput, addSender, accessibility, notifications, stop, resume)
            .forEach { view ->
                content.addView(
                    view,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = padding / 3 },
                )
            }
        return ScrollView(this).apply { addView(content) }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    RuntimeContainer.engine.state.collect { state: AutomationState ->
                        statusText.text = "Status: $state"
                    }
                }
                launch {
                    RuntimeContainer.settingsRepository.settings.collect { settings ->
                        if (enableSwitch.isChecked != settings.enabled) enableSwitch.isChecked = settings.enabled
                        val position = AiProvider.values().indexOf(settings.aiProvider)
                        if (position >= 0 && providerSpinner.selectedItemPosition != position) {
                            providerSpinner.setSelection(position)
                        }
                        providerInitialized = true
                    }
                }
            }
        }
    }

    private val AiProvider.displayName: String
        get() = when (this) {
            AiProvider.CHATGPT -> "ChatGPT"
            AiProvider.GEMINI -> "Gemini"
        }
}
