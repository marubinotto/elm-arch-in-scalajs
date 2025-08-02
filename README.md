# Elm Architecture TodoMVC

A TodoMVC implementation using the Elm Architecture pattern in Scala.js with Cats Effect.

## Project Structure

```
├── build.sbt                          # SBT build configuration
├── project/
│   ├── build.properties              # SBT version
│   └── plugins.sbt                   # Scala.js plugin
├── src/
│   ├── main/scala/
│   │   ├── architecture/              # Core Elm Architecture implementation
│   │   ├── vdom/                      # Virtual DOM system
│   │   ├── todomvc/                   # TodoMVC application
│   │   └── Main.scala                 # Application entry point
│   └── test/scala/
│       ├── architecture/              # Architecture tests
│       ├── vdom/                      # Virtual DOM tests
│       └── todomvc/                   # TodoMVC tests
├── index.html                         # HTML entry point
└── README.md                          # This file
```

## Dependencies

- **Cats Effect 3.5.2**: Functional programming with IO for side effects
- **QuickLens 1.9.6**: Immutable data transformations
- **Scala.js DOM 2.4.0**: DOM bindings for Scala.js
- **ScalaTest 3.2.17**: Testing framework
- **ScalaCheck**: Property-based testing

## Getting Started

1. Install SBT (Scala Build Tool)
2. Run `sbt fastOptJS` to compile the project
3. Open `index.html` in a web browser

## Development

- `sbt fastOptJS` - Compile for development (fast compilation)
- `sbt fullOptJS` - Compile for production (optimized)
- `sbt test` - Run tests
- `sbt ~fastOptJS` - Watch mode for development

