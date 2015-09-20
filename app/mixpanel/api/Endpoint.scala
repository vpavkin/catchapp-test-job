package mixpanel.api

sealed trait Endpoint {
  def toURL: String
}

object Endpoint {
  case object Events extends Endpoint {
    def toURL: String = "/events"
  }
}
