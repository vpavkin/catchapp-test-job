import com.softwaremill.macwire.MacwireMacros._
import config.Module
import play.api.{Application, GlobalSettings}

object Global extends GlobalSettings {

  val wired = wiredInModule(Module)
  override def getControllerInstance[A](controllerClass: Class[A]) = wired.lookupSingleOrThrow(controllerClass)

  override def onStart(app: Application): Unit = startJobs()

  private def startJobs(): Unit = Module.jobs.foreach(_.start())
}
