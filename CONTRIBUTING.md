# Contributing to Auto Reply Tools

## Before You Start

Every contributor MUST read `DEVELOPMENT_STANDARD.md` before building, modifying, or contributing to the project.

## Required Development Flow

`CODE → BUILD → INSTALL → RUN → TEST → VERIFY → PASS → NEXT STAGE`

Do not move to the next stage without verification.

## Pull Request Expectations

Every change should explain:
- What changed
- Why it changed
- How it was tested
- What was verified
- Any regression risk

## Architecture Rules

- Use functional/dynamic node matching.
- Avoid fixed coordinates and fragile resource IDs.
- Use Accessibility actions instead of coordinate taps.
- Keep AI providers behind adapters.
- Keep automation state-driven.
- Preserve whitelist, duplicate guard, watchdog, verification, and emergency STOP behavior.

## Review Rule

**No Build → Test → Verify → Pass = No Merge.**
