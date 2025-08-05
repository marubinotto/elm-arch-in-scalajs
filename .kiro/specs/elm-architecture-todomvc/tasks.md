# Implementation Plan

- [x] 1. Set up project structure and dependencies
  - Create Scala.js project with build.sbt configuration
  - Add dependencies for Cats Effect, QuickLens, ScalaTest, and Scala.js DOM
  - Set up project directory structure for core architecture and TodoMVC app
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Implement core virtual DOM types and creation functions
  - Create VNode sealed trait with VElement and VText case classes
  - Implement Html object with element creation functions (div, input, button, text)
  - Create Events object with IO-based event handlers (onClick, onInput, onKeyDown)
  - Write unit tests for virtual DOM node creation and structure
  - _Requirements: 2.1, 2.2_

- [x] 3. Implement virtual DOM diffing algorithm
  - Create Patch sealed trait with different patch types (Replace, UpdateText, UpdateAttrs, etc.)
  - Implement VDom.diff function to compare old and new virtual DOM trees
  - Write comprehensive unit tests for diffing algorithm with various node changes
  - Add property-based tests to verify diff idempotency and correctness
  - _Requirements: 2.3_

- [x] 4. Implement DOM patching and rendering system
  - Create VDom.patch function to apply patches to real DOM elements using IO
  - Implement VDom.createElement function to create DOM elements from VNodes
  - Add event listener attachment and removal with proper IO handling
  - Write integration tests for DOM manipulation operations
  - _Requirements: 2.4_

- [x] 5. Create core architecture types and abstractions
  - Implement Update case class for model and command results
  - Create App trait with init, update, view, and subscriptions methods
  - Implement Cmd sealed trait with CmdNone, CmdBatch, and CmdTask variants
  - Create Sub sealed trait with SubNone, SubBatch, and SubInterval variants
  - Add Cmd and Sub companion objects with helper functions
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 6. Implement application runtime with concurrent message processing
  - Create Runtime class that manages model state using Cats Effect Ref
  - Implement message queue processing with Queue for handling TodoMsg
  - Add concurrent rendering loop that updates DOM when model changes
  - Implement command execution pipeline for side effects
  - Create subscription management system for external events
  - Write unit tests for runtime message processing and state management
  - _Requirements: 1.5_

- [x] 7. Define TodoMVC data models and message types
  - Create Todo case class with id, text, completed, and editing fields
  - Implement TodoModel case class with todos list, newTodoText, filter, editingTodo, and editText
  - Define TodoFilter sealed trait with All, Active, and Completed objects
  - Create comprehensive TodoMsg sealed trait with all user action messages
  - Add side effect messages for LoadTodos, SaveComplete, AutoSave, and NetworkError
  - _Requirements: 3.4, 4.1, 5.1, 6.1, 7.1, 8.1_

- [x] 8. Implement TodoMVC update function with QuickLens
  - Create TodoApp object extending App[TodoModel, TodoMsg]
  - Implement init method returning initial model and load command
  - Write update method handling AddTodo with input validation and storage command
  - Add ToggleTodo case with QuickLens for toggling completion status
  - Implement UpdateNewTodoText for real-time input updates
  - Add SetFilter case for changing todo visibility filter
  - Write unit tests for each update case with various model states
  - _Requirements: 3.1, 3.2, 3.3, 4.2, 4.3, 7.2, 7.3, 7.4, 7.5_

- [x] 9. Implement remaining TodoMVC update cases
  - Add DeleteTodo case using QuickLens to remove todos by ID
  - Implement EditTodo case to enter edit mode for specific todo
  - Create UpdateTodo case with validation and QuickLens for text updates
  - Add ToggleAll case to mark all todos as completed or incomplete
  - Implement ClearCompleted case to remove all completed todos
  - Handle side effect messages (LoadTodos, SaveComplete, AutoSave)
  - Write comprehensive unit tests for all update cases
  - _Requirements: 5.2, 5.3, 5.4, 5.5, 6.2, 6.3, 4.4_

- [x] 10. Create TodoMVC view function with virtual DOM
  - Implement view method that takes TodoModel and returns VNode
  - Create header section with new todo input field and onInput event
  - Build main section with todo list filtering based on current filter
  - Add individual todo item rendering with checkbox, text, and delete button
  - Implement edit mode rendering with input field and event handlers
  - Create footer with item count, filter buttons, and clear completed button
  - Write unit tests for view function output with different model states
  - _Requirements: 3.1, 4.1, 5.1, 6.1, 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4_

- [x] 11. Implement local storage integration for persistence
  - Create LocalStorage object with getItem and setItem functions using IO
  - Add JSON serialization/deserialization for Todo and TodoModel types
  - Implement loadTodosFromStorage function returning IO[TodoMsg]
  - Create saveTodosToStorage function for persisting todos with IO
  - Add error handling for storage operations with proper fallbacks
  - Write integration tests for storage operations
  - _Requirements: 3.4, 4.2, 4.3, 5.5, 6.2, 6.3_

- [x] 12. Add subscriptions for auto-save functionality
  - Implement subscriptions method in TodoApp for periodic auto-save
  - Create subscription processing in Runtime for interval-based events
  - Add auto-save logic that triggers every 30 seconds when todos exist
  - Implement subscription cleanup and management
  - Write tests for subscription behavior and auto-save functionality
  - _Requirements: 3.4, 4.2, 4.3, 5.5, 6.2, 6.3_

- [x] 13. Create TodoMVC HTML structure and CSS integration
  - Create index.html with proper TodoMVC structure and CSS links
  - Add standard TodoMVC CSS styling from the official specification
  - Ensure HTML structure matches TodoMVC requirements for compatibility
  - Implement proper semantic HTML elements and accessibility attributes
  - Write tests to verify HTML structure compliance
  - _Requirements: 9.1, 9.2, 9.3_

- [x] 14. Implement main application entry point
  - Create Main object with Scala.js main method
  - Initialize TodoApp and Runtime with proper error handling
  - Mount application to DOM container element
  - Add proper application startup sequence with initial command execution
  - Implement graceful error handling for application initialization
  - Write integration tests for complete application startup
  - _Requirements: 1.5, 9.4_

- [x] 15. Add comprehensive error handling and edge cases
  - Implement error boundaries for update function failures
  - Add validation for edge cases like empty todos, invalid IDs, and malformed input
  - Create error recovery mechanisms for DOM operation failures
  - Add proper error messages and user feedback for failed operations
  - Implement graceful degradation when local storage is unavailable
  - Write tests for all error conditions and edge cases
  - _Requirements: 9.4_

- [ ] 16. Create comprehensive test suite and fix remaining E2E issues
  - [x] Write property-based tests for model invariants (unique IDs, valid states)
  - [x] All Scala unit tests pass (109/109 tests passing)
  - [ ] **Fix remaining E2E test failures** (CRITICAL - 9/57 tests still failing)
    - **Root cause**: Event handling issues in browser environment causing user interactions to fail
    - **Impact**: Adding multiple todos fails, checkbox toggling doesn't work, UI interactions timeout
    - **Current status**: VDom event system implemented but browser integration has issues
    - **Solution**: Debug and fix event handling in actual browser environment
  - [ ] Add integration tests for complete user workflows (add, edit, delete, filter)
  - [ ] Create performance tests for large todo lists and frequent updates
  - [ ] Implement browser compatibility tests for DOM operations
  - [ ] Write tests for concurrent operations and race conditions
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4_

- [x] 17. Fix VDom event system for E2E test compatibility
- [x] 17.1 Complete Events.scala implementation with real DOM event handling
  - Replace placeholder onInput handler with real DOM event capture
  - Implement proper value extraction from input events using event.target.value
  - Replace placeholder onKeyDown handler with real keyCode detection
  - Add proper event preventDefault and stopPropagation handling
  - Ensure all event handlers properly dispatch messages to the Runtime
  - _Requirements: 2.4, 9.4_

- [x] 17.2 Fix VDom.attachEventListeners to work with message dispatching
  - Modify attachEventListeners to accept message dispatch function
  - Update event listener creation to properly dispatch TodoMsg messages
  - Ensure event handlers have access to current model state when needed
  - Add error handling for event dispatch failures
  - Test that DOM events properly trigger model updates
  - _Requirements: 2.4, 1.5_

- [x] 17.3 Update TodoApp view to use working VDom events
  - Remove placeholder event handlers from TodoApp view methods
  - Replace custom attachEventListeners approach with working VDom events
  - Ensure all user interactions (input, keydown, click, dblclick) work correctly
  - Update event handlers to pass proper message constructors
  - Test that Enter key on new-todo input dispatches AddTodo message
  - _Requirements: 3.1, 4.1, 5.1, 6.1, 7.1_

- [x] 17.4 Integrate fixed event system with Runtime
  - Update Runtime to pass message dispatch function to VDom.attachEventListeners
  - Ensure event listeners are attached after initial render and updates
  - Remove redundant custom event attachment code from Runtime
  - Add logging to verify event listeners are properly attached
  - Test that all TodoMVC interactions work in browser
  - _Requirements: 1.5, 2.4_

- [ ] 17.5 Debug and fix remaining E2E test failures
  - **Current status**: 9/57 E2E tests failing, 48/57 passing
  - **Issues to fix**:
    - Adding multiple todos only adds 2 out of 3 (timing/async issue)
    - Checkbox toggling doesn't work (event not triggering model updates)
    - Various UI interactions timeout (event handlers not properly attached)
  - Debug event handling in browser environment using browser dev tools
  - Fix async timing issues with todo addition
  - Ensure checkbox change events properly dispatch ToggleTodo messages
  - Verify all event listeners are properly attached to DOM elements
  - Test each failing scenario individually and fix root causes
  - Achieve 100% E2E test pass rate (57/57 tests passing)
  - _Requirements: 9.4_

- [ ] 18. Investigate and fix specific E2E test failure patterns
  - [ ] 18.1 Fix "should add multiple todos" test failure
    - **Issue**: Only 2 out of 3 todos are being added when test tries to add 3
    - **Root cause**: Likely async timing issue with rapid todo additions
    - **Solution**: Debug the AddTodo message handling and ensure proper async processing
    - Add proper waiting/debouncing for rapid user input
    - Ensure each AddTodo message is fully processed before the next one
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [ ] 18.2 Fix checkbox toggling functionality
    - **Issue**: Clicking todo checkboxes doesn't toggle completion status
    - **Root cause**: onChange events not properly dispatching ToggleTodo messages
    - **Solution**: Debug the checkbox event handling in browser
    - Verify that onChange events are properly attached to checkbox elements
    - Ensure ToggleTodo messages are being dispatched and processed
    - Test that model updates are reflected in the DOM
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [ ] 18.3 Fix keyboard navigation test failures
    - **Issue**: Space key on checkboxes doesn't toggle todos (all browsers)
    - **Root cause**: Keyboard event handling not working for checkbox interactions
    - **Solution**: Add proper keydown event handling for Space key on checkboxes
    - Ensure keyboard accessibility for todo toggling
    - Test that both click and keyboard interactions work
    - _Requirements: 4.1, 9.4_
  
  - [ ] 18.4 Fix filter and clear completed functionality
    - **Issue**: Tests timeout when trying to interact with todo items for filtering
    - **Root cause**: Event handlers not properly attached to todo list items
    - **Solution**: Debug event attachment in todo list rendering
    - Ensure all todo item interactions (toggle, delete, edit) work properly
    - Verify that filter buttons and clear completed button work
    - _Requirements: 5.2, 6.1, 6.2, 7.1, 7.2, 7.3_