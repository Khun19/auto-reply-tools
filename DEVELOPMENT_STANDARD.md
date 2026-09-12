# Development Standard

## 1. Purpose

This document is mandatory for every developer working on Auto Reply Tools. It defines the engineering rules, automation architecture, testing gates, safety requirements, and review standards for the project.

## 2. Non-Negotiable Principles

1. Do not rely on fixed screen coordinates for automation.
2. Do not rely on fragile resource IDs when functional node matching can be used.
3. Prefer AccessibilityNodeInfo actions over coordinate taps.
4. Automation must be state-driven and recoverable.
5. Every feature must pass Build → Test → Verify → Pass before the next stage.
6. A successful build is not feature success.
7. Never bypass duplicate protection, whitelist checks, watchdogs, or emergency STOP controls.

## 3. Standard Automation Flow

Incoming Viber Notification
↓
NotificationListenerService
↓
Whitelist + Duplicate Guard
↓
Automation State Machine
↓
Open ChatGPT / Gemini
↓
AccessibilityService
↓
Find editable node dynamically
↓
ACTION_SET_TEXT / PASTE
↓
Find functional Send node
↓
ACTION_CLICK
↓
WAIT_AI_RESPONSE
↓
Detect generation finished
↓
Extract latest AI response
↓
Open Viber via Intent
↓
Find Viber input dynamically
↓
ACTION_SET_TEXT / PASTE
↓
Find Send
↓
ACTION_CLICK
↓
VERIFY
↓
IDLE

## 4. Development Gate

Every stage follows:

`CODE → BUILD → INSTALL → RUN → TEST → VERIFY → PASS → NEXT STAGE`

No Build → Test → Verify → Pass = No Merge.

## 5. Accessibility Engine

Use functional node matching and scoring instead of fixed IDs.

### Input Node

Preferred signals:
- `isEditable == true`
- `className == "android.widget.EditText"`
- Visible and enabled
- Appropriate bounds/container context

### Send Node

Preferred signals:
- Clickable
- Visible and enabled
- Near the input/container
- Content description or text semantically matching Send, Submit, or equivalent

### Supported Actions

Prefer:
- `ACTION_SET_TEXT`
- `ACTION_PASTE`
- `ACTION_CLICK`
- IME send where appropriate

For `ACTION_SET_TEXT`, use `AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE` as the argument key.

## 6. AI Adapter Architecture

Use an adapter interface so AI providers are replaceable without rewriting the automation engine.

Recommended components:
- `AiAppAdapter`
- `ChatGptAdapter`
- `GeminiAdapter`
- `ViberAdapter`
- `AutomationEngine`
- `AutomationState`
- `NodeMatcher`
- `NodeScorer`
- `TextExtractor`
- `Watchdog`
- `DuplicateGuard`

## 7. State Machine

Recommended states:

- `IDLE`
- `VIBER_MSG_DETECTED`
- `OPEN_AI_APP`
- `PASTE_TO_AI`
- `SEND_TO_AI`
- `WAIT_AI_RESPONSE`
- `COPY_AI_RESPONSE`
- `OPEN_VIBER`
- `PASTE_TO_VIBER`
- `SEND_TO_VIBER`
- `VERIFY`
- `ERROR`
- `STOPPED`

Every transition must have a clear success condition, timeout, and recovery path where applicable.

## 8. AI Response Detection

Do not assume the last TextView is always the AI response.

Use generation-state detection, stable-text polling, semantic extraction, and bounded timeouts. Avoid arbitrary fixed sleeps as the primary synchronization mechanism.

Recommended polling interval: approximately 250–500 ms, with a maximum operation timeout appropriate to the AI provider.

## 9. Safety Requirements

### Whitelist
Only process notifications from approved Viber contacts/chats.

### Duplicate Guard
Use message hash and/or timestamp plus cooldown protection to prevent duplicate replies.

### Watchdog
Every long-running automation operation must have a timeout and recovery path.

### Emergency STOP
Provide an accessible emergency STOP mechanism, such as a visible floating control or accessibility overlay, capable of stopping automation immediately.

## 10. App Switching

Prefer direct application Intents for switching between Viber and the selected AI app. Do not depend on Home-screen navigation as the normal flow.

## 11. Error Recovery

Errors must transition to a known state. The engine must not remain silently stuck.

Examples:
- Node not found → retry with bounded timeout → ERROR
- AI generation timeout → STOP/ERROR and recover
- Viber input not found → retry → ERROR
- Verification failure → prevent uncontrolled repeat sending

## 12. Logging

Logs should be structured around:
- Current state
- Event
- Attempt number
- Node match result
- Action result
- Timeout/error
- Recovery decision

Never log passwords, authentication tokens, private credentials, or unnecessary private message content.

## 13. Testing

Each stage must include:

1. Unit tests where applicable
2. Accessibility/node matching tests
3. State transition tests
4. Timeout and recovery tests
5. Duplicate guard tests
6. End-to-end device testing for real UI automation

## 14. Regression Testing

Changes to adapters, node matching, state transitions, or notification handling must be tested against previously working flows.

UI changes in Viber, ChatGPT, or Gemini must not be handled by blindly adding coordinates. Update functional matching and adapter logic instead.

## 15. Prohibited Shortcuts

Do not:
- Hard-code screen coordinates as the primary automation mechanism.
- Assume fixed resource IDs across app versions.
- Use arbitrary long sleeps instead of state/polling logic.
- Assume the last TextView is always the correct response.
- Remove safety guards to make a demo appear to work.
- Merge without verification evidence.

## 16. Milestones

- v0.1 Core Foundation
- v0.2 Accessibility Engine
- v0.3 Notification Engine
- v0.4 State Machine
- v0.5 ChatGPT Adapter
- v0.6 Viber Adapter
- v0.7 AI Response Detection
- v0.8 Full Auto Reply
- v0.9 Safety & Recovery
- v1.0 Production Release

Each milestone must pass the development gate before moving forward.

## 17. Code Review Requirements

A reviewer should verify:
- Architecture compliance
- Dynamic node matching
- Correct Accessibility actions
- State transitions and timeouts
- Safety guards
- Duplicate protection
- Logging quality
- Tests and verification evidence
- Regression impact

## 18. Developer Acknowledgement

Before contributing, every developer must read this document and follow it.

## Final Engineering Rule

**Build it. Test it. Verify it. Prove it works. Then move to the next stage.**
