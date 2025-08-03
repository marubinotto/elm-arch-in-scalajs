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

- [ ] 15. Add comprehensive error handling and edge cases
  - Implement error boundaries for update function failures
  - Add validation for edge cases like empty todos, invalid IDs, and malformed input
  - Create error recovery mechanisms for DOM operation failures
  - Add proper error messages and user feedback for failed operations
  - Implement graceful degradation when local storage is unavailable
  - Write tests for all error conditions and edge cases
  - _Requirements: 9.4_

- [ ] 16. Create comprehensive test suite
  - Write property-based tests for model invariants (unique IDs, valid states)
  - Add integration tests for complete user workflows (add, edit, delete, filter)
  - Create performance tests for large todo lists and frequent updates
  - Implement browser compatibility tests for DOM operations
  - Add end-to-end tests simulating real user interactions
  - Write tests for concurrent operations and race conditions
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4_