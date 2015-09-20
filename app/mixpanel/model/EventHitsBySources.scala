package mixpanel.model

case class EventHitsBySources(event: Event, hits: Map[EventSource, BigDecimal])
