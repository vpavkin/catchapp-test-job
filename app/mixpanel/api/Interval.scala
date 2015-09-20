package mixpanel.api

case class Interval(unit: Interval.Unit, amount: Int)

object Interval {

  sealed trait Unit
  case object minute extends Unit
  case object hour extends Unit
  case object day extends Unit
  case object week extends Unit
  case object month extends Unit
}
