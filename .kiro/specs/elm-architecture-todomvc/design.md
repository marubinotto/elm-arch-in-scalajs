# Design Document

## Overview

This design implements the Elm Architecture pattern in Scala.js to create a functional, immutable web application framework. The architecture consists of three main components: Model (application state), Update (state transitions), and View (UI rendering). The design includes a virtual DOM implementation for efficient rendering and a TodoMVC application as a practical demonstration.

The implementation leverages Scala.js for type safety and functional programming paradigms while targeting web browsers, enhanced with Cats Effect for pure functional programming with proper effect management. The virtual DOM system provides declarative UI programming with efficient DOM updates through diffing algorithms, while Cats Effect handles side effects like DOM manipulation and event handling in a purely functional manner. QuickLens is used for elegant immutable data transformations, making nested model updates concise and readable.

## Architecture

### Core Architecture Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Elm Architecture                         │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐    ┌──────────┐    ┌─────────┐    ┌─────────┐  │
│  │  View   │───▶│   Msg    │───▶│ Update  │───▶│  Model  │  │
│  │         │    │          │    │         │    │         │  │
│  └─────────┘    └──────────┘    └─────────┘    └─────────┘  │
│       ▲                                             │       │
│       └─────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

**Architecture Improvements**: The view function now takes an optional dispatch parameter, eliminating the need for separate `setDispatch` methods and making the architecture more functional and composable. The Runtime passes the dispatch function directly to the view when rendering interactive elements.

### Layer Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   TodoMVC Application                       │
├─────────────────────────────────────────────────────────────┤
│                   Elm Architecture                          │
├─────────────────────────────────────────────────────────────┤
│                   Virtual DOM System                        │
├─────────────────────────────────────────────────────────────┤
│                      Scala.js                               │
├─────────────────────────────────────────────────────────────┤
│                   Browser DOM API                           │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Core Architecture Types

```scala
import cats.effect.IO
import cats.effect.kernel.Ref
import cats.data.State

// Enhanced architecture with Commands and Subscriptions
case class Update[Model, Msg](model: Model, cmd: Cmd[Msg])

trait App[Model, Msg] {
  def init: (Model, Cmd[Msg])
  def update(msg: Msg, model: Model): Update[Model, Msg]
  def view(model: Model, dispatch: Option[Msg => IO[Unit]] = None): VNode
  def subscriptions(model: Model): Sub[Msg] = Sub.none
}

// Commands for side effects
sealed trait Cmd[+Msg]
case object CmdNone extends Cmd[Nothing]
case class CmdBatch[Msg](cmds: List[Cmd[Msg]]) extends Cmd[Msg]
case class CmdTask[Msg](task: IO[Msg]) extends Cmd[Msg]

// Subscriptions for external events
sealed trait Sub[+Msg]
case object SubNone extends Sub[Nothing]
case class SubBatch[Msg](subs: List[Sub[Msg]]) extends Sub[Msg]
case class SubInterval[Msg](duration: FiniteDuration, msg: Msg) extends Sub[Msg]

object Cmd {
  def none[Msg]: Cmd[Msg] = CmdNone
  def task[Msg](io: IO[Msg]): Cmd[Msg] = CmdTask(io)
  def batch[Msg](cmds: Cmd[Msg]*): Cmd[Msg] = CmdBatch(cmds.toList)
}

object Sub {
  def none[Msg]: Sub[Msg] = SubNone
  def interval[Msg](duration: FiniteDuration, msg: Msg): Sub[Msg] = SubInterval(duration, msg)
  def batch[Msg](subs: Sub[Msg]*): Sub[Msg] = SubBatch(subs.toList)
}

// Virtual DOM node representation
sealed trait VNode
case class VElement(tag: String, attrs: Map[String, String], 
                   events: Map[String, IO[Msg]], children: List[VNode]) extends VNode
case class VText(text: String) extends VNode
```

### 2. Virtual DOM System

```scala
// Virtual DOM creation functions
object Html {
  def div(attrs: (String, String)*)(children: VNode*): VNode
  def input(attrs: (String, String)*): VNode
  def button(attrs: (String, String)*)(children: VNode*): VNode
  def text(content: String): VNode
}

// Event handling with IO
object Events {
  def onClick[Msg](msg: IO[Msg]): (String, IO[Msg])
  def onInput[Msg](f: String => IO[Msg]): (String, IO[Msg])
  def onKeyDown[Msg](f: Int => IO[Option[Msg]]): (String, IO[Msg])
}
```

### 3. Runtime System

```scala
import cats.effect.kernel.Ref
import cats.effect.std.Queue
import cats.effect.implicits._
import fs2.Stream

// Enhanced runtime with Commands and Subscriptions
class Runtime[Model, Msg](app: App[Model, Msg]) {
  def start(container: Element): IO[Unit] = for {
    (initialModel, initialCmd) <- IO.pure(app.init)
    modelRef <- Ref.of[IO, Model](initialModel)
    msgQueue <- Queue.unbounded[IO, Msg]
    subRef <- Ref.of[IO, Sub[Msg]](Sub.none)
    
    // Start all concurrent processes
    _ <- List(
      processMessages(modelRef, msgQueue, subRef),
      renderLoop(modelRef, container, msgQueue),
      subscriptionLoop(subRef, msgQueue)
    ).parSequence.start
    
    // Execute initial command
    _ <- executeCmd(initialCmd, msgQueue)
  } yield ()
  
  private def processMessages(modelRef: Ref[IO, Model], msgQueue: Queue[IO, Msg], subRef: Ref[IO, Sub[Msg]]): IO[Unit]
  private def renderLoop(modelRef: Ref[IO, Model], container: Element, msgQueue: Queue[IO, Msg]): IO[Unit]
  private def subscriptionLoop(subRef: Ref[IO, Sub[Msg]], msgQueue: Queue[IO, Msg]): IO[Unit]
  private def executeCmd(cmd: Cmd[Msg], msgQueue: Queue[IO, Msg]): IO[Unit]
  
  def dispatch(msg: Msg): IO[Unit]
}

// DOM diffing and patching with IO
object VDom {
  def diff(oldNode: VNode, newNode: VNode): List[Patch]
  def patch(element: Element, patches: List[Patch]): IO[Unit]
  def createElement(vnode: VNode): IO[Element]
}
```

### 4. TodoMVC Application Model

```scala
import com.softwaremill.quicklens._

// TodoMVC specific types
case class Todo(id: Int, text: String, completed: Boolean, editing: Boolean)

case class TodoModel(
  todos: List[Todo],
  newTodoText: String,
  filter: TodoFilter,
  editingTodo: Option[Int],
  editText: String
)

sealed trait TodoFilter
case object All extends TodoFilter
case object Active extends TodoFilter
case object Completed extends TodoFilter

// Enhanced TodoMVC messages with side effects
sealed trait TodoMsg
case class AddTodo(text: String) extends TodoMsg
case class ToggleTodo(id: Int) extends TodoMsg
case class DeleteTodo(id: Int) extends TodoMsg
case class EditTodo(id: Int) extends TodoMsg
case class UpdateTodo(id: Int, text: String) extends TodoMsg
case class SetFilter(filter: TodoFilter) extends TodoMsg
case class UpdateNewTodoText(text: String) extends TodoMsg
case object ToggleAll extends TodoMsg
case object ClearCompleted extends TodoMsg

// Side effect messages
case class LoadTodos(todos: List[Todo]) extends TodoMsg
case object SaveComplete extends TodoMsg
case object AutoSave extends TodoMsg
case class NetworkError(error: String) extends TodoMsg

// Enhanced TodoApp with Commands and better separation
object TodoApp extends App[TodoModel, TodoMsg] {
  def init: (TodoModel, Cmd[TodoMsg]) = {
    val initialModel = TodoModel(
      todos = List.empty,
      newTodoText = "",
      filter = All,
      editingTodo = None,
      editText = ""
    )
    (initialModel, Cmd.task(loadTodosFromStorage))
  }
  
  def update(msg: TodoMsg, model: TodoModel): Update[TodoModel, TodoMsg] = msg match {
    case AddTodo(text) if text.trim.nonEmpty =>
      val newTodo = Todo(generateId(), text.trim, completed = false, editing = false)
      val updatedModel = model
        .modify(_.todos).using(_ :+ newTodo)
        .modify(_.newTodoText).setTo("")
      Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))
    
    case ToggleTodo(id) =>
      val updatedModel = model.modify(_.todos.each.when(_.id == id).completed).using(!_)
      Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))
    
    case UpdateNewTodoText(text) =>
      Update(model.modify(_.newTodoText).setTo(text), Cmd.none)
    
    case SetFilter(filter) =>
      Update(model.modify(_.filter).setTo(filter), Cmd.none)
      
    case LoadTodos(todos) =>
      Update(model.modify(_.todos).setTo(todos), Cmd.none)
      
    // ... other cases
  }
  
  def view(model: TodoModel, dispatch: Option[TodoMsg => IO[Unit]] = None): VNode = {
    // Render the current state as virtual DOM
    // Use dispatch function for interactive elements when provided
    div("class" -> "todoapp")(
      dispatch match {
        case Some(dispatchFn) =>
          List(
            renderHeaderWithDispatch(model, dispatchFn),
            renderMainWithDispatch(model, dispatchFn),
            renderFooterWithDispatch(model, dispatchFn)
          )
        case None =>
          List(
            renderHeader(model),
            renderMain(model),
            renderFooter(model)
          )
      }*
    )
  }

  def subscriptions(model: TodoModel): Sub[TodoMsg] = {
    // Auto-save every 30 seconds if there are changes
    if (model.todos.nonEmpty) {
      Sub.interval(30.seconds, AutoSave)
    } else {
      Sub.none
    }
  }
  
  // Side effect functions
  private def loadTodosFromStorage: IO[TodoMsg] = 
    LocalStorage.getItem("todos").map(LoadTodos.apply)
    
  private def saveTodosToStorage(todos: List[Todo]): IO[TodoMsg] =
    LocalStorage.setItem("todos", todos).as(SaveComplete)
}
```

## Data Models

### Virtual DOM Node Structure

The virtual DOM uses a tree structure where each node can be either an element or text:

- **VElement**: Contains tag name, attributes, event handlers, and child nodes
- **VText**: Contains plain text content
- **Attributes**: Key-value pairs for HTML attributes
- **Events**: Map event names to message constructors

### TodoMVC State Model

The application state is immutable and contains:

- **todos**: List of all todo items with unique IDs
- **newTodoText**: Current text in the new todo input field
- **filter**: Current filter setting (All/Active/Completed)
- **editingTodo**: ID of todo currently being edited (if any)
- **editText**: Text content during editing

### Message Flow

All state changes flow through typed messages:

1. User interactions generate messages
2. Messages are dispatched to the update function
3. Update function returns new immutable state
4. New state triggers view re-rendering
5. Virtual DOM diffing optimizes actual DOM updates

## Design Principles

### Type Safety and Fail-Fast Philosophy

The architecture follows strict type safety principles and fail-fast error handling:

1. **Make Invalid States Unrepresentable**: Use the type system to prevent invalid states rather than validating them at runtime
2. **Fail-Fast on Programming Errors**: Let bugs surface clearly during development rather than masking them with recovery mechanisms
3. **Developer Responsibility**: Update functions must maintain model invariants - violations are bugs, not recoverable errors
4. **No Runtime Validation**: Avoid defensive programming that hides bugs behind fallback mechanisms

### Type-Driven Design Examples

```scala
// Instead of runtime validation, use types that prevent invalid states

// Bad: Runtime validation for positive IDs
case class Todo(id: Int, text: String, completed: Boolean)
def validateTodoId(id: Int): Either[ValidationError, Int] = 
  if (id <= 0) Left(ValidationError("id", "Must be positive", id)) else Right(id)

// Good: Use refined types or smart constructors
opaque type TodoId = Int
object TodoId {
  def apply(value: Int): TodoId = {
    require(value > 0, "TodoId must be positive")
    value
  }
}

// Bad: List allows duplicates, requires runtime validation
case class TodoModel(todos: List[Todo], ...)

// Good: Map prevents duplicates by design
case class TodoModel(todos: Map[TodoId, Todo], ...)

// Bad: Optional ID reference that might not exist
case class TodoModel(..., editingTodo: Option[Int])

// Good: Direct reference that guarantees existence
case class TodoModel(..., editingTodo: Option[Todo])
```

### Contract-Based Programming

```scala
// Update functions have a contract: (Msg, Model) => Update[Model, Msg]
// If this contract is violated (e.g., returning null), it's a programming error
// The type system enforces non-null references in Scala

trait App[Model, Msg] {
  // Contract: Must return valid Update with non-null model
  def update(msg: Msg, model: Model): Update[Model, Msg]
  
  // Contract: Must return valid VNode
  def view(model: Model, dispatch: Option[Msg => IO[Unit]]): VNode
}
```

## Error Handling

### Principled Error Handling

Error handling distinguishes between:

1. **Programming Errors**: Type errors, contract violations, logic bugs → Fail fast
2. **Runtime Errors**: Network failures, DOM issues, external service problems → Handle gracefully
3. **User Errors**: Invalid input, business rule violations → Validate at boundaries

```scala
import cats.effect.IO
import cats.syntax.all._

// Handle external/runtime errors gracefully
def safeDispatch(msg: Msg): IO[Unit] = 
  dispatch(msg).handleErrorWith { 
    case e: Exception => IO.println(s"Error dispatching message: ${e.getMessage}")
  }

// Error types for runtime issues only
sealed trait RuntimeError extends Throwable
case class NetworkError(message: String) extends RuntimeError
case class DOMError(message: String) extends RuntimeError  
case class StorageError(message: String) extends RuntimeError

// Error recovery for external failures only
def withErrorRecovery[A](io: IO[A], fallback: A): IO[A] = 
  io.handleErrorWith(_ => IO.pure(fallback))
```

### Input Sanitization (Not Validation)

```scala
import com.softwaremill.quicklens._

// Sanitize user input at boundaries, don't validate internal state
def sanitizeUserInput(text: String): String = 
  Option(text).getOrElse("").trim

def updateTodo(id: TodoId, newText: String, model: TodoModel): TodoModel = {
  val sanitizedText = sanitizeUserInput(newText)
  if (sanitizedText.isEmpty) {
    model.modify(_.todos).using(_ - id)  // Remove empty todos
  } else {
    model.modify(_.todos).using(_.updated(id, 
      model.todos(id).copy(text = sanitizedText, editing = false)))
  }
}
```

- Sanitize user input at system boundaries
- Use types that prevent invalid states by construction
- No runtime validation of internal model consistency
- QuickLens enables safe nested transformations with compile-time guarantees

### DOM Operation Safety

- Null checks for DOM elements
- Safe event listener attachment/removal
- Graceful handling of missing DOM nodes during patching
- Recovery from DOM manipulation failures

## Testing Strategy

### Unit Testing

```scala
// Model testing
class TodoModelSpec extends AnyFunSpec {
  describe("TodoModel") {
    it("should add new todos correctly") {
      val model = TodoModel.init
      val updated = TodoApp.update(AddTodo("Test"), model)
      assert(updated.todos.length == 1)
      assert(updated.todos.head.text == "Test")
    }
  }
}

// Update function testing
class TodoUpdateSpec extends AnyFunSpec {
  describe("update function") {
    it("should handle all message types") {
      // Test each message type with various model states
    }
  }
}
```

### Virtual DOM Testing

```scala
// Virtual DOM diffing tests with property-based testing
class VDomSpec extends AnyFunSpec with ScalaCheckPropertyChecks {
  describe("Virtual DOM") {
    it("should generate correct patches for changes") {
      val oldNode = div()(text("old"))
      val newNode = div()(text("new"))
      val patches = VDom.diff(oldNode, newNode)
      // Assert correct patch operations
    }
    
    property("diff should be idempotent") {
      forAll { (node: VNode) =>
        VDom.diff(node, node) shouldBe empty
      }
    }
    
    property("applying patches should result in equivalent DOM") {
      forAll { (oldNode: VNode, newNode: VNode) =>
        val patches = VDom.diff(oldNode, newNode)
        val element = VDom.createElement(oldNode).unsafeRunSync()
        VDom.patch(element, patches).unsafeRunSync()
        // Verify element matches newNode structure
      }
    }
  }
}
```

### Integration Testing

```scala
// Full application flow testing
class TodoAppIntegrationSpec extends AnyFunSpec {
  describe("TodoMVC App") {
    it("should handle complete user workflows") {
      // Test adding, editing, completing, filtering todos
      // Simulate user interactions and verify state changes
    }
  }
}
```

### Browser Testing

- DOM manipulation verification
- Event handling correctness
- Cross-browser compatibility
- Performance benchmarking for large todo lists

### Property-Based Testing

```scala
// Property-based tests for model invariants
class TodoPropertiesSpec extends AnyPropSpec {
  property("todo IDs should always be unique") {
    forAll { (actions: List[TodoMsg]) =>
      val finalModel = actions.foldLeft(TodoModel.init)(TodoApp.update)
      val ids = finalModel.todos.map(_.id)
      ids.distinct.length == ids.length
    }
  }
}
```

### Cats Effect Testing

```scala
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.IO

// Testing IO-based update functions
class TodoUpdateIOSpec extends AsyncIOSpec {
  "update function" should "handle messages in IO" in {
    val model = TodoModel.init
    TodoApp.update(AddTodo("Test"), model).asserting { updated =>
      updated.todos.length shouldBe 1
      updated.todos.head.text shouldBe "Test"
    }
  }
}

// Testing runtime behavior
class RuntimeSpec extends AsyncIOSpec {
  "Runtime" should "process messages correctly" in {
    val runtime = new Runtime(TodoApp)
    for {
      _ <- runtime.start(mockContainer)
      _ <- runtime.dispatch(AddTodo("Test"))
      // Verify state changes through IO
    } yield succeed
  }
}
```

The testing strategy ensures correctness at all levels: pure functions (update), virtual DOM operations, DOM integration, and complete user workflows. Property-based testing validates invariants across random input sequences. Cats Effect testing utilities provide proper testing of IO-based operations and concurrent behavior.