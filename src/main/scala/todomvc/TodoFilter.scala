package todomvc

/** Filter options for displaying todos
  */
sealed trait TodoFilter {

  /** Get the display name for this filter */
  def displayName: String

  /** Check if a todo should be visible with this filter */
  def matches(todo: Todo): Boolean
}

/** Show all todos regardless of completion status */
case object All extends TodoFilter {
  def displayName: String = "All"
  def matches(todo: Todo): Boolean = true
}

/** Show only active (not completed) todos */
case object Active extends TodoFilter {
  def displayName: String = "Active"
  def matches(todo: Todo): Boolean = !todo.completed
}

/** Show only completed todos */
case object Completed extends TodoFilter {
  def displayName: String = "Completed"
  def matches(todo: Todo): Boolean = todo.completed
}

object TodoFilter {

  /** All available filter options */
  val all: List[TodoFilter] = List(All, Active, Completed)

  /** Parse a filter from a string name */
  def fromString(name: String): Option[TodoFilter] = name.toLowerCase match {
    case "all"       => Some(All)
    case "active"    => Some(Active)
    case "completed" => Some(Completed)
    case _           => None
  }
}
