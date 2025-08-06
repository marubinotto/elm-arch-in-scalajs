# Elm Architecture in Scala.js

This is an experimental implementation of the Elm Architecture in Scala.js, developed in collaboration with [Kiro](https://kiro.dev/) (Claude Sonnet 4.0).

A TodoMVC implementation demonstrating the Elm Architecture pattern in Scala.js with Cats Effect. This project showcases how functional programming principles and the Elm Architecture can be applied to build robust, maintainable web applications using Scala.js.

## The Elm Architecture

The Elm Architecture is a pattern for architecting interactive programs that emphasizes:

### Core Principles

1. **Unidirectional Data Flow**: Data flows in one direction through the application, making state changes predictable and debuggable.

2. **Immutable State**: The application state (Model) is immutable and can only be changed by creating new versions.

3. **Pure Functions**: Update functions are pure - they take a message and current state, returning a new state and any side effects to perform.

4. **Separation of Concerns**: Clear separation between state management (Model), state transitions (Update), user interface (View), and side effects (Commands and Subscriptions).

### Architecture Components

The Elm Architecture consists of four main components:

#### Model
The **Model** represents the complete state of your application at any point in time. It's an immutable data structure that contains all the information needed to render the current view.

```scala
case class TodoModel(
  todos: List[Todo],
  newTodoText: String,
  filter: TodoFilter,
  editingTodo: Option[Int],
  editText: String
)
```

#### Messages (Msg)
**Messages** represent all the ways your application state can change. They are typically implemented as a sealed trait with case classes for each possible action.

```scala
sealed trait TodoMsg
case class AddTodo(text: String) extends TodoMsg
case class ToggleTodo(id: Int) extends TodoMsg
case class DeleteTodo(id: Int) extends TodoMsg
// ... more message types
```

#### Update
The **Update** function is a pure function that takes a message and the current model, returning a new model and any commands to execute. This is where all state transitions happen.

```scala
def update(msg: TodoMsg, model: TodoModel): Update[TodoModel, TodoMsg] = {
  msg match {
    case AddTodo(text) => 
      // Create new model with added todo
      // Return commands for side effects (e.g., save to storage)
    case ToggleTodo(id) =>
      // Create new model with toggled todo
      // Return commands for persistence
    // ... handle other messages
  }
}
```

#### View
The **View** function is a pure function that takes the current model and returns a virtual DOM representation of the user interface.

```scala
def view(model: TodoModel): VNode = {
  // Render the current state as virtual DOM
  div("class" -> "todoapp")(
    renderHeader(model),
    renderMain(model),
    renderFooter(model)
  )
}
```

#### Commands and Subscriptions
- **Commands** represent side effects that should be performed (HTTP requests, local storage operations, etc.)
- **Subscriptions** represent ongoing effects like timers, WebSocket connections, or keyboard events

## Elm Architecture with Scala.js and Cats Effect

This implementation adapts the Elm Architecture to leverage Scala.js and Cats Effect's powerful abstractions:

### Cats Effect Integration

#### IO for Side Effects
All side effects are wrapped in `IO` monads, providing:
- **Referential Transparency**: Side effects are values that can be composed and reasoned about
- **Error Handling**: Built-in error handling with `handleErrorWith`
- **Concurrency**: Safe concurrent execution of multiple effects
- **Resource Management**: Automatic cleanup of resources

```scala
// Commands return IO operations
case class CmdTask[Msg](task: IO[Msg]) extends Cmd[Msg]

// Side effects like storage operations
private def saveTodosToStorage(todos: List[Todo]): IO[TodoMsg] = {
  LocalStorage.saveTodos(todos)
    .as(SaveComplete)
    .handleErrorWith { error =>
      IO.delay(console.error(s"Save failed: ${error.getMessage}")) *>
        IO.pure(SaveComplete)
    }
}
```

#### Concurrent Runtime System
The runtime system uses Cats Effect's concurrency primitives:

```scala
class Runtime[Model, Msg](app: App[Model, Msg]) {
  def start(container: Element): IO[Unit] = {
    for {
      modelRef <- Ref.of[IO, Model](initialModel)
      msgQueue <- Queue.unbounded[IO, Msg]
      
      // Start concurrent processes
      messageProcessor <- processMessages(modelRef, msgQueue).start
      renderer <- renderLoop(modelRef, container).start
      subscriptionManager <- subscriptionLoop(subRef, msgQueue).start
      
      _ <- executeCmd(initialCmd, msgQueue)
    } yield ()
  }
}
```

### Functional Programming Benefits

#### Immutable Data Structures
Using QuickLens for immutable updates:

```scala
// Safe, immutable state updates
val updatedModel = model
  .modify(_.todos.eachWhere(_.id == id).completed)
  .using(!_)
```

#### Error Handling and Validation
Comprehensive validation with custom error types:

```scala
object TodoValidation {
  def validateTodoText(text: String): Either[ValidationError, String] = {
    val trimmed = text.trim
    if (trimmed.isEmpty) Left(ValidationError("Todo text cannot be empty"))
    else if (trimmed.length > 500) Left(ValidationError("Todo text too long"))
    else Right(trimmed)
  }
}
```

#### Type Safety
Strong typing prevents runtime errors:
- Sealed traits ensure exhaustive pattern matching
- Case classes provide immutable data structures
- Type parameters ensure message/model consistency

### Virtual DOM Implementation

Custom Virtual DOM system optimized for the Elm Architecture:

```scala
sealed trait VNode
case class VElement(tag: String, attrs: Map[String, String], 
                   events: Map[String, Event => IO[Unit]], 
                   children: List[VNode]) extends VNode

object VDom {
  def diff(oldNode: VNode, newNode: VNode): List[Patch] = {
    // Efficient diffing algorithm
  }
  
  def patch(element: Node, patches: List[Patch]): IO[Unit] = {
    // Apply patches to real DOM
  }
}
```

### Subscription System

Event-driven subscriptions using Cats Effect:

```scala
sealed trait Sub[+Msg]
case class SubInterval[Msg](duration: FiniteDuration, msg: Msg) extends Sub[Msg]

// Auto-save subscription
override def subscriptions(model: TodoModel): Sub[TodoMsg] = {
  if (model.todos.nonEmpty) {
    Sub.interval(30.seconds, AutoSave)
  } else {
    Sub.none
  }
}
```

## Building and Running TodoMVC

### Prerequisites
- **SBT (Scala Build Tool)**: For compiling Scala.js code
- **Node.js and npm**: For development server and build tools
- **Modern web browser**: Chrome, Firefox, Safari, or Edge

### Development Setup

1. **Clone and install dependencies:**
   ```bash
   git clone <repository-url>
   cd elm-arch-in-scalajs
   npm install
   ```

2. **Start development server:**
   ```bash
   npm run dev
   ```
   This starts Vite development server with hot reloading at http://localhost:3000

3. **Alternative SBT-only development:**
   ```bash
   sbt ~fastOptJS    # Watch mode compilation
   # Then serve index.html with any static server
   ```

### Building for Production

```bash
npm run build       # Build optimized bundle
npm run preview     # Preview production build
```

### Testing

#### Unit Tests
```bash
sbt test           # Run Scala unit tests
```

#### End-to-End Tests
```bash
npm run test:e2e          # Run Playwright tests headless
npm run test:e2e:headed   # Run with browser visible
npm run test:e2e:ui       # Run with Playwright UI
```

### Project Structure

```
├── build.sbt                          # SBT build configuration
├── package.json                       # Node.js dependencies and scripts
├── vite.config.js                     # Vite configuration for Scala.js
├── playwright.config.js               # E2E test configuration
├── src/
│   ├── main/scala/
│   │   ├── architecture/              # Core Elm Architecture implementation
│   │   │   ├── package.scala         # Core types (App, Cmd, Sub, Update)
│   │   │   ├── Runtime.scala         # Concurrent runtime system
│   │   │   └── ErrorHandling.scala   # Error recovery and boundaries
│   │   ├── vdom/                      # Virtual DOM system
│   │   │   ├── VNode.scala           # Virtual DOM node types
│   │   │   ├── VDom.scala            # Diffing and patching
│   │   │   ├── Html.scala            # HTML DSL
│   │   │   └── Events.scala          # Event handling
│   │   ├── todomvc/                   # TodoMVC application
│   │   │   ├── TodoApp.scala         # Main application logic
│   │   │   ├── TodoModel.scala       # Application state
│   │   │   ├── TodoMsg.scala         # Message types
│   │   │   ├── Todo.scala            # Todo data type
│   │   │   ├── TodoFilter.scala      # Filter types (All/Active/Completed)
│   │   │   ├── TodoValidation.scala  # Input validation
│   │   │   ├── LocalStorage.scala    # Browser storage integration
│   │   │   └── TodoJson.scala        # JSON serialization
│   │   └── Main.scala                 # Application entry point
│   └── test/scala/                    # Unit tests
├── e2e/                               # End-to-end tests
│   └── todomvc.spec.js               # Playwright test suite
├── styles/                            # CSS styles
│   └── custom.css                    # TodoMVC styling
└── index.html                         # HTML entry point
```

## Future Improvements

### Architecture Enhancements

1. **Time Travel Debugging**
   - Implement message history tracking
   - Add replay functionality for debugging
   - Create dev tools for state inspection

2. **Advanced Error Boundaries**
   - Component-level error isolation
   - Graceful degradation strategies
   - User-friendly error reporting

3. **Performance Optimizations**
   - Virtual DOM memoization
   - Selective re-rendering based on model changes
   - Lazy loading for large todo lists

### Feature Additions

1. **Enhanced Todo Management**
   - Todo categories and tags
   - Due dates and reminders
   - Priority levels
   - Bulk operations (select multiple, batch edit)

2. **Data Persistence**
   - Server-side synchronization
   - Offline support with conflict resolution
   - Export/import functionality
   - Multiple storage backends

3. **User Experience**
   - Drag and drop reordering
   - Keyboard shortcuts
   - Undo/redo functionality
   - Search and filtering

### Technical Improvements

1. **Testing Infrastructure**
   - Property-based testing with ScalaCheck
   - Visual regression testing
   - Performance benchmarking
   - Accessibility testing automation

2. **Development Experience**
   - Hot module replacement for Scala.js
   - Better error messages and debugging
   - Code generation for boilerplate
   - IDE integration improvements

3. **Production Readiness**
   - Bundle size optimization
   - Progressive Web App features
   - Monitoring and analytics
   - Internationalization support

### Ecosystem Integration

1. **State Management**
   - Integration with Redux DevTools
   - Middleware system for logging/analytics
   - State persistence strategies

2. **Routing**
   - Client-side routing integration
   - Deep linking support
   - Browser history management

3. **Styling**
   - CSS-in-Scala solutions
   - Theme system
   - Responsive design utilities

This TodoMVC implementation demonstrates how the Elm Architecture can be successfully adapted to Scala.js while leveraging the power of Cats Effect for functional programming. The result is a robust, maintainable, and type-safe web application that showcases best practices in functional reactive programming.

