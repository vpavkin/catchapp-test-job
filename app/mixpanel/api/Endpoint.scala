package mixpanel.api

sealed trait Endpoint {
  def toURL: String
}

object Endpoint {
  case object Events extends Endpoint {
    def toURL: String = "events"
  }
  case object EventsProperties extends Endpoint {
    def toURL: String = "events/properties"
  }
  case object EventsPropertiesValues extends Endpoint {
    def toURL: String = "events/properties/values"
  }
}
