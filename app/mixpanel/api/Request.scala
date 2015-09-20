package mixpanel.api

import com.github.nscala_time.time.Imports.{Interval => _, _}
import utils.md5

sealed trait Request {
  def endpoint: Endpoint
  def expire: DateTime
  def params: List[(String, String)]
}

object Request {

  implicit class ListOps(l: List[String]) {
    def toJsonArrayString = s"[${l.map(e => s""""$e"""").mkString(",")}]"
  }

  case class Events(
                     events: List[String],
                     analysisType: AnalysisType,
                     interval: Interval,
                     expire: DateTime = DateTime.nextHour) extends Request {
    def endpoint: Endpoint = Endpoint.Events
    def params: List[(String, String)] = List(
      "event" -> events.toJsonArrayString,
      "type" -> analysisType.toString,
      "unit" -> interval.unit.toString,
      "interval" -> interval.amount.toString
    )
  }

  case class EventsProperties(event: String,
                              propertyName: String,
                              analysisType: AnalysisType,
                              interval: Interval,
                              propertyValues: Option[List[String]] = None,
                              limit: Int = 255,
                              expire: DateTime = DateTime.nextHour) extends Request {

    def endpoint: Endpoint = Endpoint.EventsProperties
    def params: List[(String, String)] = propertyValues.map("values" -> _.toJsonArrayString).toList ++ List(
      "event" -> event,
      "name" -> propertyName,
      "type" -> analysisType.toString,
      "unit" -> interval.unit.toString,
      "interval" -> interval.amount.toString
    )
  }

  case class EventsPropertiesValues(event: String,
                                    propertyName: String,
                                    limit: Int = 255,
                                    expire: DateTime = DateTime.nextHour) extends Request {
    def endpoint: Endpoint = Endpoint.EventsPropertiesValues
    def params: List[(String, String)] = List(
      "event" -> event,
      "name" -> propertyName,
      "limit" -> limit.toString
    )
  }
}
