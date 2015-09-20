package mixpanel.api.parsers

import mixpanel.model.{EventSource, Event, EventHitsBySources}
import play.api.Logger
import play.api.libs.json.{JsNumber, JsObject, JsValue}

class MergedEventSourcesHitsParser(eventName: String) extends ResponseParser[EventHitsBySources] {

  def parse(response: JsValue): Option[EventHitsBySources] = response \ "data" \ "values" match {
    case JsObject(sources) =>
      Logger.logger.warn(response.toString())
      Some(EventHitsBySources(
        Event(eventName),
        sources.flatMap {
          case (sourceName, hits: JsObject) =>
            Seq((EventSource(sourceName), mergeHits(hits)))
          case _ => Seq.empty
        }.toMap
      ))
  }

  private def mergeHits(hits: JsObject): BigDecimal =
    hits.fields.foldLeft(BigDecimal(0)) {
      case (agg, hit) => hit match {
        case (_, JsNumber(number)) => agg + number
        case _ => agg
      }
    }
}
