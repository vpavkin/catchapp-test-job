package catchapp.mailer.controllers.misc

import play.api.libs.json.Json
import play.api.mvc.{Controller, Action}

class VersionController() extends Controller {

  def version = Action {
    Ok(Json.obj("version" -> "1.0.0-SHANPSHOT"))
  }
}
