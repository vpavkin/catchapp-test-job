package mixpanel.api

case class Interval(amount: Int, unit: Interval.Unit)

object Interval {

  sealed trait Unit
  case object minute extends Unit
  case object hour extends Unit
  case object day extends Unit
  case object week extends Unit
  case object month extends Unit
}
