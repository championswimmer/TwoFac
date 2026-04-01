# Testing Revamp for cliApp

## Goals
1. Review existing tests and delete trivial tests.
2. Add tests for complex CLI parse flows and execution paths.
3. Mock external dependencies.

## Progress
- [x] Review `KoinVerificationTest.kt`
- [x] Review `DisplayCommandTest.kt` (Deleted, trivial)
- [x] Review `BackupCommandTest.kt` (Kept, solid flow tests)
- [x] Review `StorageCommandTest.kt` (Kept)
- [x] Review `InfoCommandTest.kt` (Deleted, trivial)
- [x] Identify and remove trivial tests
- [x] Add new complex flow tests (`AddCommandTest`, `ParsingFlowTest`)
