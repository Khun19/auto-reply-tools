# Auto Reply Tools

Android automation toolkit for Viber auto-replies using AccessibilityService, NotificationListenerService, AI adapters, and a resilient state-machine architecture.

## Developer Notice

**Every developer MUST read and follow `DEVELOPMENT_STANDARD.md` before building, modifying, or contributing to this project.**

**No Build → Test → Verify → Pass = No Merge.**

## Architecture

- Android Native
- AccessibilityService
- NotificationListenerService
- State Machine
- Dynamic Functional Node Matching
- ChatGPT / Gemini adapter architecture
- Viber adapter
- Watchdog and duplicate protection
- Verification and recovery states

## Standard Automation Flow

Incoming Viber Notification
→ NotificationListenerService
→ Whitelist + Duplicate Guard
→ Automation State Machine
→ Open ChatGPT / Gemini
→ AccessibilityService
→ Find editable node dynamically
→ Set text / paste
→ Find functional Send node
→ Click
→ Wait for AI response
→ Extract latest AI response
→ Open Viber
→ Find Viber input dynamically
→ Set text / paste
→ Find Send
→ Click
→ Verify
→ IDLE

## Development Gate

`CODE → BUILD → INSTALL → RUN → TEST → VERIFY → PASS → NEXT STAGE`

A successful build alone does not mean a feature is complete.
