# composeApp Testing Revamp

## Goals
- [x] Review existing tests
- [x] Delete trivial tests (OTPCardTest)
- [ ] Add tests for untested complex flows (viewmodels, coordinators)
- [ ] Mock out coordinators/providers

## Progress
- Removed trivial `OTPCardTest` which was testing simple utility functions.
- Cleaned up `WatchSyncCoordinatorTest`.
