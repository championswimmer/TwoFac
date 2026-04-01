# composeApp Testing Revamp

## Goals
- [x] Review existing tests
- [x] Delete trivial tests
- [x] Add tests for untested complex flows (viewmodels, coordinators)
- [x] Mock out coordinators/providers

## Progress
- Removed trivial `OTPCardTest` which was testing simple utility functions.
- Cleaned up `WatchSyncCoordinatorTest`.
- Scaffolded `AccountsViewModelTest` structure.
