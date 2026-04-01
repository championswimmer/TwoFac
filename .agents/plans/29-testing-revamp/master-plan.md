# Testing Revamp Plan

## Goals
1. Review existing tests and delete trivial ones (e.g. basic string mappings, obvious getters).
2. Add new tests for more involved flows, mocking out dependencies like sync coordinators and backup providers.
3. Check test coverage to find gaps.
4. Coordinate work across different modules to avoid conflicts.
5. Commit in small pieces.

## Sub-agent Assignments
- **sharedLib & composeApp**: FastTiger
- **cliApp / watchApp**: SageUnion
