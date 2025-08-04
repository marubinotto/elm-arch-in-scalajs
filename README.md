# Elm Architecture TodoMVC

A TodoMVC implementation using the Elm Architecture pattern in Scala.js with Cats Effect.

## Project Structure

```
├── build.sbt                          # SBT build configuration
├── package.json                       # Node.js dependencies and scripts
├── vite.config.js                     # Vite configuration
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

### Scala Dependencies
- **Cats Effect 3.5.2**: Functional programming with IO for side effects
- **QuickLens 1.9.6**: Immutable data transformations
- **Scala.js DOM 2.4.0**: DOM bindings for Scala.js
- **ScalaTest 3.2.17**: Testing framework
- **ScalaCheck**: Property-based testing

### Development Dependencies
- **Vite**: Fast development server and build tool
- **@scala-js/vite-plugin-scalajs**: Vite plugin for Scala.js integration

## Getting Started

### Prerequisites
- SBT (Scala Build Tool)
- Node.js and npm

### Setup and Run
1. **Install Node.js dependencies:**
   ```bash
   npm install
   ```

2. **Start the development server:**
   ```bash
   npm run dev
   ```

3. **Open your browser** to http://localhost:3000 (opens automatically)

## Development

- `npm run dev` - Start development server with hot reloading
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `sbt test` - Run tests

### Alternative SBT-only Development

- `sbt fastOptJS` - Compile for development (fast compilation)
- `sbt fullOptJS` - Compile for production (optimized)
- `sbt ~fastOptJS` - Watch mode for development

