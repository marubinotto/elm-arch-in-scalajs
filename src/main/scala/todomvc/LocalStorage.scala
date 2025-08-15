package todomvc

import cats.effect.IO
import org.scalajs.dom
import scala.scalajs.js
import scala.util.{Try, Success, Failure}

/** Local storage utilities for persisting TodoMVC data
  */
object LocalStorage {

  /** Storage key for todos */
  private val TODOS_KEY = "todomvc-todos"

  /** Get an item from local storage
    *
    * @param key
    *   The storage key
    * @return
    *   IO operation that returns the stored value or None if not found
    */
  def getItem(key: String): IO[Option[String]] = {
    IO.delay {
      Option(dom.window.localStorage.getItem(key)).filter(_.nonEmpty)
    }.handleErrorWith { error =>
      // Log error and return None for graceful degradation
      IO.delay(
        dom.console
          .error(s"Failed to read from localStorage: ${error.getMessage}")
      ) *>
        IO.pure(None)
    }
  }

  /** Set an item in local storage
    *
    * @param key
    *   The storage key
    * @param value
    *   The value to store
    * @return
    *   IO operation that completes when the item is stored
    */
  def setItem(key: String, value: String): IO[Unit] = {
    IO.delay {
      dom.window.localStorage.setItem(key, value)
    }.handleErrorWith { error =>
      // Log error and continue for graceful degradation
      IO.delay(
        dom.console
          .error(s"Failed to write to localStorage: ${error.getMessage}")
      )
    }
  }

  /** Remove an item from local storage
    *
    * @param key
    *   The storage key
    * @return
    *   IO operation that completes when the item is removed
    */
  def removeItem(key: String): IO[Unit] = {
    IO.delay {
      dom.window.localStorage.removeItem(key)
    }.handleErrorWith { error =>
      // Log error and continue for graceful degradation
      IO.delay(
        dom.console
          .error(s"Failed to remove from localStorage: ${error.getMessage}")
      )
    }
  }

  /** Load todos from local storage with comprehensive error handling
    *
    * @return
    *   IO operation that returns the list of todos
    */
  def loadTodos: IO[List[Todo]] = {
    // First check if localStorage is available
    isAvailable
      .flatMap { available =>
        if (!available) {
          IO.delay(
            dom.console
              .warn("localStorage is not available, using empty todo list")
          ) *>
            IO.pure(List.empty[Todo])
        } else {
          getItem(TODOS_KEY).flatMap {
            case Some(jsonString) =>
              IO.delay {
                if (jsonString.trim.isEmpty) {
                  List.empty[Todo]
                } else {
                  TodoJson.parseTodos(jsonString) match {
                    case Success(todos) =>
                      // Validate loaded todos
                      TodoValidation.validateTodoList(todos) match {
                        case Right(validTodos) => validTodos
                        case Left(error) =>
                          List.empty[Todo]
                      }
                    case Failure(error) =>
                      // Try to recover by clearing corrupted data (fire and forget)
                      import cats.effect.unsafe.implicits.global
                      clearTodos.attempt.void.unsafeRunAndForget()
                      List.empty[Todo]
                  }
                }
              }.handleErrorWith { error =>
                IO.println(
                  s"Error processing todos from localStorage: ${error.getMessage}"
                ) *>
                  IO.pure(List.empty[Todo])
              }
            case None =>
              IO.pure(List.empty[Todo])
          }
        }
      }
      .handleErrorWith { error =>
        IO.println(s"Critical error loading todos: ${error.getMessage}") *>
          IO.pure(List.empty[Todo])
      }
  }

  /** Save todos to local storage with comprehensive error handling
    *
    * @param todos
    *   The list of todos to save
    * @return
    *   IO operation that completes when todos are saved
    */
  def saveTodos(todos: List[Todo]): IO[Unit] = {
    // First validate the todos
    TodoValidation.validateTodoList(todos) match {
      case Left(error) =>
        IO.println(s"Cannot save invalid todos: ${error.userMessage}")
      case Right(validTodos) =>
        // Check if localStorage is available
        isAvailable
          .flatMap { available =>
            if (!available) {
              IO.println(
                "localStorage is not available, todos will not be persisted"
              )
            } else {
              IO.delay {
                TodoJson.serializeTodos(validTodos)
              }.flatMap { jsonString =>
                // Check if JSON is reasonable size (< 5MB)
                if (jsonString.length > 5 * 1024 * 1024) {
                  IO.println("Todo data is too large to save to localStorage")
                } else {
                  setItem(TODOS_KEY, jsonString).handleErrorWith { error =>
                    // Try to handle quota exceeded error
                    if (
                      error.getMessage.contains(
                        "QuotaExceededError"
                      ) || error.getMessage.contains("quota")
                    ) {
                      IO.println(
                        "localStorage quota exceeded, cannot save todos"
                      ) *>
                        // Try to clear some space by removing old data
                        clearTodos *>
                        setItem(TODOS_KEY, jsonString).handleErrorWith { _ =>
                          IO.println(
                            "Failed to save todos even after clearing storage"
                          )
                        }
                    } else {
                      IO.println(
                        s"Failed to save todos to localStorage: ${error.getMessage}"
                      )
                    }
                  }
                }
              }.handleErrorWith { error =>
                IO.println(s"Failed to serialize todos: ${error.getMessage}")
              }
            }
          }
          .handleErrorWith { error =>
            IO.println(s"Critical error saving todos: ${error.getMessage}")
          }
    }
  }

  /** Clear all todos from local storage
    *
    * @return
    *   IO operation that completes when todos are cleared
    */
  def clearTodos: IO[Unit] = {
    removeItem(TODOS_KEY)
  }

  /** Check if local storage is available
    *
    * @return
    *   IO operation that returns true if localStorage is available
    */
  def isAvailable: IO[Boolean] = {
    IO.delay {
      Try {
        val testKey = "todomvc-test"
        val testValue = "test"
        dom.window.localStorage.setItem(testKey, testValue)
        val retrieved = dom.window.localStorage.getItem(testKey)
        dom.window.localStorage.removeItem(testKey)
        retrieved == testValue
      }.getOrElse(false)
    }
  }
}
