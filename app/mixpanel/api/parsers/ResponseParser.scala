package mixpanel.api.parsers

import play.api.libs.json.JsValue

trait ResponseParser[A] {
  def parse(response: JsValue): Option[A]
}
