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

  /** Load todos from local storage
    *
    * @return
    *   IO operation that returns the list of todos
    */
  def loadTodos: IO[List[Todo]] = {
    getItem(TODOS_KEY).flatMap {
      case Some(jsonString) =>
        IO.delay {
          TodoJson.parseTodos(jsonString) match {
            case Success(todos) => todos
            case Failure(error) =>
              dom.console.error(
                s"Failed to parse todos from localStorage: ${error.getMessage}"
              )
              List.empty[Todo]
          }
        }
      case None =>
        IO.pure(List.empty[Todo])
    }
  }

  /** Save todos to local storage
    *
    * @param todos
    *   The list of todos to save
    * @return
    *   IO operation that completes when todos are saved
    */
  def saveTodos(todos: List[Todo]): IO[Unit] = {
    IO.delay {
      TodoJson.serializeTodos(todos)
    }.flatMap { jsonString =>
      setItem(TODOS_KEY, jsonString)
    }.handleErrorWith { error =>
      // Log error and continue for graceful degradation
      IO.delay(
        dom.console
          .error(s"Failed to save todos to localStorage: ${error.getMessage}")
      )
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
