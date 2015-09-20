package mixpanel.api

import mixpanel.api.parsers.ResponseParser
import play.api.libs.json._
import utils.md5
import dispatch._, Defaults._
import scala.util.Try
import scalaz._
import Scalaz._
import scala.concurrent.Future

object MixpanelAPI {

  val API_HOST = "mixpanel.com"
  val BASE_URL = host(API_HOST) / "api" / "2.0"

  trait Error {
    def msg: String
  }

  object Error {
    case object InvalidJsonReceived extends Error {
      def msg = "Received invalid JSON from mixpanel"
    }
    case object ParserFailure extends Error {
      def msg = "Parser failed to interpret received data"
    }
  }

  implicit class RequestOps(r: Request) {

    def fullParams(apiKey: String): List[(String, String)] =
      (("expire" -> (r.expire.getMillis / 1000).toString) ::
        ("api_key" -> apiKey) :: r.params).sortBy(_._1)

    private def sigString(apiKey: String, apiSecret: String): String = fullParams(apiKey)
      .map(pair => pair._1 + "=" + pair._2)
      .mkString("") + apiSecret

    def sig(apiKey: String, apiSecret: String): String = md5(sigString(apiKey, apiSecret))
  }
}

class MixpanelAPI(val key: String, val secret: String) {

  import MixpanelAPI._

  def send(request: Request): Future[String] = {
    val httpRequest =
      (BASE_URL / request.endpoint.toURL <<? request.fullParams(key))
        .addQueryParameter("sig", request.sig(key, secret))

    Http(httpRequest OK as.String)
  }

  def send[A](request: Request, parser: ResponseParser[A]): Future[Error \/ A] = {
    send(request).map(rawResult =>
      Try(Json.parse(rawResult)) match {
        case scala.util.Failure(exception) => Error.InvalidJsonReceived.left
        case scala.util.Success(value) => parser.parse(value) match {
          case Some(result) => result.right
          case None => Error.ParserFailure.left
        }
      }
    )
  }
}
