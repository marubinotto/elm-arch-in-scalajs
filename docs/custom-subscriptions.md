# Custom Subscriptions Guide

This guide explains how to implement and use custom subscriptions in the Elm Architecture framework.

## Overview

Subscriptions allow your application to listen to external events like keyboard input, mouse clicks, WebSocket messages, or any other asynchronous event source.

## Built-in Subscription Types

### 1. Interval Subscription
```scala
Sub.interval(1.second, Tick)
```
Sends a message at regular intervals.

### 2. Keyboard Subscription
```scala
Sub.keyboard(key => KeyPressed(key))
```
Listens for keyboard events and sends messages with the pressed key.

### 3. Mouse Subscription
```scala
Sub.mouse((x, y) => MouseClicked(x, y))
```
Listens for mouse click events and sends messages with click coordinates.

### 4. WebSocket Subscription
```scala
Sub.webSocket(
  "ws://localhost:8080/ws",
  data => WebSocketMessage(data),
  error => WebSocketError(error)
)
```
Connects to a WebSocket and sends messages for incoming data and errors.

### 5. Custom Subscription
```scala
Sub.custom("my-subscription", { dispatch =>
  IO.delay {
    // Setup your event source here
    val cleanup = setupEventSource(dispatch)
    
    // Return cleanup function
    IO.delay(cleanup())
  }
})
```
Allows you to create completely custom event sources.

## Implementing Custom Subscriptions

### Step 1: Add to the Sub sealed trait

```scala
case class SubMyCustom[Msg](config: MyConfig, handler: MyEvent => Msg) extends Sub[Msg]
```

### Step 2: Add helper method to Sub companion object

```scala
object Sub {
  def myCustom[Msg](config: MyConfig, handler: MyEvent => Msg): Sub[Msg] = 
    SubMyCustom(config, handler)
}
```

### Step 3: Handle in Runtime.startSubscriptions

```scala
case SubMyCustom(config, handler) =>
  val customId = s"mycustom_${config.hashCode}"
  val customLoop = setupMyCustomListener(config, handler, msgQueue)
  customLoop.start.map(fiber => Map(customId -> fiber))
```

### Step 4: Implement the setup function

```scala
private def setupMyCustomListener[Msg](
    config: MyConfig,
    handler: MyEvent => Msg,
    msgQueue: Queue[IO, Msg]
): IO[Unit] = {
  IO.async_[Unit] { cb =>
    // Setup your event listener
    val listener = (event: MyEvent) => {
      val msg = handler(event)
      msgQueue.offer(msg).unsafeRunAndForget()
    }
    
    // Register the listener
    MyEventSource.addListener(listener)
    
    // The subscription runs until the fiber is cancelled
    // Cleanup happens automatically when cancelled
  }
}
```

## Usage in Applications

```scala
override def subscriptions(model: Model): Sub[Msg] = {
  Sub.batch(
    Sub.interval(1.second, Tick),
    Sub.keyboard(key => KeyPressed(key)),
    Sub.mouse((x, y) => MouseClicked(x, y)),
    Sub.custom("my-events", setupMyEvents)
  )
}
```

## Best Practices

1. **Use descriptive IDs** for custom subscriptions to avoid conflicts
2. **Handle cleanup properly** in custom subscriptions to prevent memory leaks
3. **Batch subscriptions** when you need multiple event sources
4. **Test subscriptions** by verifying the correct messages are generated
5. **Keep subscription logic simple** - complex logic should be in the update function

## Testing Custom Subscriptions

```scala
"should create custom subscription" in {
  val sub = Sub.myCustom(config, handler)
  sub shouldBe a[SubMyCustom[_]]
  // Test the handler function
  sub.handler(testEvent) shouldBe expectedMessage
}
```

## Common Patterns

### Conditional Subscriptions
```scala
override def subscriptions(model: Model): Sub[Msg] = {
  if (model.isListening) {
    Sub.keyboard(key => KeyPressed(key))
  } else {
    Sub.none
  }
}
```

### Dynamic Subscriptions
```scala
override def subscriptions(model: Model): Sub[Msg] = {
  val baseSubs = List(Sub.interval(1.second, Tick))
  val conditionalSubs = model.activeFeatures.map {
    case "keyboard" => Sub.keyboard(KeyPressed)
    case "mouse" => Sub.mouse(MouseClicked)
    case _ => Sub.none
  }
  Sub.batch((baseSubs ++ conditionalSubs): _*)
}
```