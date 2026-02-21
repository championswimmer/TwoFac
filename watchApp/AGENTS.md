# watchApp AGENTS.md

`watchApp` is the Android Wear OS companion app module.

## Current state

- Uses Android + Compose for Wear.
- Current implementation is a minimal starter-style UI in:
  - `src/main/java/tech/arnav/twofac/watch/presentation/MainActivity.kt`
  - `src/main/java/tech/arnav/twofac/watch/presentation/theme/`

## Dependencies

- Depends on `sharedLib` for 2FA core logic.
- Uses `kstore` and appdirs dependencies, preparing for persisted data integration.

## Notes for future work

- Most production account/OTP logic should stay in `sharedLib`.
- Keep Wear-specific UI and lifecycle code isolated in this module.
