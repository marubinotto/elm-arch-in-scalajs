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
  def view(model: Model): VNode
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
      renderLoop(modelRef, container),
      subscriptionLoop(subRef, msgQueue)
    ).parSequence.start
    
    // Execute initial command
    _ <- executeCmd(initialCmd, msgQueue)
  } yield ()
  
  private def processMessages(modelRef: Ref[IO, Model], msgQueue: Queue[IO, Msg], subRef: Ref[IO, Sub[Msg]]): IO[Unit]
  private def renderLoop(modelRef: Ref[IO, Model], container: Element): IO[Unit]
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

## Error Handling

### Runtime Error Handling

```scala
import cats.effect.IO
import cats.syntax.all._

// Safe message dispatching with Cats Effect error handling
def safeDispatch(msg: Msg): IO[Unit] = 
  dispatch(msg).handleErrorWith { 
    case e: Exception => IO.println(s"Error dispatching message: ${e.getMessage}")
  }

// Error types
sealed trait AppError extends Throwable
case class RuntimeError(message: String) extends AppError
case class RenderError(message: String) extends AppError  
case class ValidationError(field: String, message: String) extends AppError

// Error recovery with IO
def withErrorRecovery[A](io: IO[A], fallback: A): IO[A] = 
  io.handleErrorWith(_ => IO.pure(fallback))
```

### Input Validation

```scala
import com.softwaremill.quicklens._

// Validation with QuickLens transformations
def validateAndUpdateTodo(id: Int, newText: String, model: TodoModel): IO[TodoModel] = {
  if (newText.trim.isEmpty) {
    IO.pure(model.modify(_.todos).using(_.filterNot(_.id == id)))
  } else {
    IO.pure(model.modify(_.todos.each.when(_.id == id))
      .using(_.modify(_.text).setTo(newText.trim)
              .modify(_.editing).setTo(false)))
  }
}
```

- Empty todo text validation before adding
- Safe DOM element access with Option types  
- Bounds checking for todo ID operations
- Event handler error boundaries
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