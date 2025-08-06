---
inclusion: always
---

# Testing Requirements

## Mandatory Zero Failing Tests Policy

**CRITICAL REQUIREMENT**: Every task must be completed with ZERO failing tests before marking the task as complete.

### Rules

1. **All tests must pass**: No exceptions. 100% test success rate is mandatory.

2. **If tests cannot be fixed**: Remove problematic tests rather than leaving them failing.

3. **Environment-specific test failures**: Tests that fail due to environment limitations (e.g., localStorage tests in Node.js) should be removed or mocked appropriately.

4. **Test validation before task completion**: Always run ALL tests (`sbt test` for unit tests and `npm run test:e2e` for e2e tests) and verify all tests pass before marking any task as completed.

5. **No partial completion**: A task is not complete until all tests pass.

### Implementation Guidelines

- Write unit tests that work in the target test environment (Node.js for Scala.js)
- Write e2e tests that verify end-to-end functionality in the browser
- Use appropriate mocking for browser-specific APIs when testing in Node.js
- Remove tests that cannot be made to work reliably rather than leaving them failing
- Focus on testing core functionality that can be verified in both unit and e2e test environments

### Verification Process

Before completing any task:

1. Run `sbt test` for unit tests
2. Run `npm run test:e2e` for e2e tests
3. Verify output shows "All tests passed" for both test suites
4. Ensure no failing, canceled, or ignored tests in any test suite
5. Only then mark the task as complete

This policy ensures code quality and prevents regression issues in the codebase.