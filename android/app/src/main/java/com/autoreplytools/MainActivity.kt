package com.autoreplytools

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autoreplytools.core.RuntimeContainer
import com.autoreplytools.core.model.AutomationState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var enableSwitch: Switch
    private lateinit var whitelistContainer: LinearLayout

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
        val whitelistTitle = TextView(this).apply {
            text = "Whitelisted Viber senders"
            textSize = 18f
            setTextColor(Color.rgb(20, 50, 45))
            setPadding(0, padding / 3, 0, padding / 6)
        }
        whitelistContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
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
        listOf(title, statusText, enableSwitch, senderInput, addSender, whitelistTitle, whitelistContainer, accessibility, notifications, stop, resume)
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
                        renderWhitelist(settings.whitelist)
                    }
                }
            }
        }
    }

    private fun renderWhitelist(senders: Set<String>) {
        whitelistContainer.removeAllViews()
        if (senders.isEmpty()) {
            whitelistContainer.addView(TextView(this).apply {
                text = "No senders added"
                setTextColor(Color.DKGRAY)
            })
            return
        }

        senders.toList().sorted().forEach { sender ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val name = TextView(this).apply {
                text = sender
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val remove = Button(this).apply {
                text = "Remove"
                setOnClickListener {
                    lifecycleScope.launch {
                        RuntimeContainer.settingsRepository.removeWhitelistSender(sender)
                    }
                }
            }
            row.addView(name)
            row.addView(remove)
            whitelistContainer.addView(row)
        }
    }
}
