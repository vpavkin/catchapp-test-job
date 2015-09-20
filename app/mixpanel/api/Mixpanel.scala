package mixpanel.api

import utils.md5

object Mixpanel {
  implicit class RequestOps(r: Request) {

    private def sigParams(apiKey: String): List[(String, String)] =
      (("expire" -> (r.expire.getMillis / 1000).toString) ::
        ("api_key" -> apiKey) :: r.params).sortBy(_._1)

    private def sigString(apiKey: String, apiSecret: String): String = sigParams(apiKey)
      .map(pair => pair._1 + "=" + pair._2)
      .mkString("") + apiSecret

    def sig(apiKey: String, apiSecret: String): String = md5(sigString(apiKey, apiSecret))

  }
}

class MixpanelAPI(val key: String, val secret: String) {
}
