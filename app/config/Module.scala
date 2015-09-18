package config

import catchapp.mailer.controllers.misc.VersionController
import catchapp.mailer.jobs.{StatsMailJob, DailyJobRunner, JobRunner}
import com.softwaremill.macwire.MacwireMacros._
import org.joda.time.format.DateTimeFormat
import play.api.Application
import play.api.libs.concurrent.Akka
import com.github.nscala_time.time.Imports.DateTime
import scala.concurrent.duration._
import com.microtripit.mandrillapp.lutung._

object Module {

  implicit def currentApplication: Application = play.api.Play.current
  def actorSystem = Akka.system

  def getOrNotify[T](getter: => T, message: String = ""): T = try {
    getter
  } catch {
    case e: Throwable => throw new Error(s"Invalid configuration. $message")
  }

  lazy val config = play.api.Play.current.configuration

  lazy val versionController: VersionController = wire[VersionController]

  lazy val jobStartTime: DateTime = getOrNotify[DateTime](DateTime.parse(
    config.getString("mailer.sendTime").get, DateTimeFormat.forPattern("HH:mm:ss")),
    "No start date"
  )
  lazy val jobExecutionTimeout = Duration(10, MINUTES)
  lazy val emailReceivers: List[String] = getOrNotify[List[String]](config.getStringSeq("mailer.receivers").get.toList, "No receivers")
  lazy val templateName: String = getOrNotify[String](config.getString("mandrill.templateName").get, "No template name")
  lazy val mandrillAPI: MandrillApi = new MandrillApi(getOrNotify[String](config.getString("mandrill.apiKey").get, "No API key"))

  lazy val statsMailJob: StatsMailJob = new StatsMailJob(mandrillAPI, templateName, emailReceivers)
  lazy val dailyMailJobRunner: JobRunner = new DailyJobRunner(actorSystem, jobStartTime, jobExecutionTimeout, statsMailJob)
  lazy val jobs: List[JobRunner] = List(dailyMailJobRunner)
}
