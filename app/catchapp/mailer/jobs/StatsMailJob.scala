package catchapp.mailer.jobs

import java.util

import catchapp.mailer.jobs.StatsMailJob.Stats
import com.github.nscala_time.time.Imports._
import com.microtripit.mandrillapp.lutung._
import com.microtripit.mandrillapp.lutung.view._
import com.microtripit.mandrillapp.lutung.view.MandrillMessage._
import play.api.Logger

import scala.concurrent.{Future, ExecutionContext}


object StatsMailJob {
  case class Stats(sends: Int, opens: Int, clicks: Int)
}

class StatsMailJob(mandrillApi: MandrillApi,
                   templateName: String,
                   receivers: List[String]) extends PeriodicalJob[Period] {
  def id: JobId = JobId("email-template-stats")

  def run(config: Period)(implicit exc: ExecutionContext): Future[Unit] =
    getHistory(config).flatMap(sendEmails(config))

  private def recipients = {
    val recs = new util.ArrayList[Recipient]()

    receivers.foreach(rec => {
      val recipient = new Recipient()
      recipient.setEmail(rec)
      recs.add(recipient)
    })
    recs
  }

  private def message(config: Period)(result: Stats) = {
    val email = new MandrillMessage()
    email.setSubject( s"""Daily "$templateName" template stats.""")
    email.setHtml( s"""
        <h3>Template "$templateName" stats
        from ${config.from.toString("dd.MM.yyyy HH:mm")}
        to ${config.to.toString("dd.MM.yyyy HH:mm")}</h3>
        <p>Sent emails: ${result.sends}</p>
        <p>Opens: ${result.opens}</p>
        <p>Clicks: ${result.clicks}</p>""")
    email.setAutoText(true)
    email.setFromEmail("mailer@catchapp.co")
    email.setFromName("Catchapp mailer")
    email.setTo(recipients)
    email.setPreserveRecipients(true)
    email
  }

  private def sendEmails(config: Period)(stats: Stats)(implicit exc: ExecutionContext): Future[Unit] = Future {
    val statuses = mandrillApi.messages().send(message(config)(stats), false).toList
    statuses.foreach(status => if (status.getStatus() == "rejected" || status.getStatus() == "invalid")
      Logger.logger.error(s"Failed to send emails: ${status.getRejectReason()}")
    )
  }

  private def getHistory(config: Period)(implicit exc: ExecutionContext): Future[Stats] = Future {
    val rawHistory: List[MandrillTimeSeries] = mandrillApi.templates.timeSeries(templateName).toList

    rawHistory.filter(ts => {
      val tsTime = new DateTime(ts.getTime())
      tsTime.isAfter(config.from) && tsTime.isBefore(config.to)
    })
      .map(ts => Stats(ts.getSent(), ts.getOpens(), ts.getClicks()))
      .foldLeft(Stats(0, 0, 0)) {
      case (aggregate, stats) =>
        Stats(
          aggregate.sends + stats.sends,
          aggregate.opens + stats.opens,
          aggregate.clicks + stats.clicks
        )
    }
  }
}
