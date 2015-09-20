package catchapp.mailer.jobs

import java.util

import catchapp.mailer.jobs.StatsMailJob.{MixpanelConfig, MandrillConfig, Stats}
import com.github.nscala_time.time.Imports._
import com.microtripit.mandrillapp.lutung._
import com.microtripit.mandrillapp.lutung.view._
import com.microtripit.mandrillapp.lutung.view.MandrillMessage._
import mixpanel.api.{Interval, AnalysisType, Request, MixpanelAPI}
import mixpanel.model.{EventSource, EventHitsBySources}
import play.api.Logger
import scalaz._
import Scalaz._
import scala.concurrent.{Future, ExecutionContext}
import mixpanel.api.parsers.MergedEventSourcesHitsParser

object StatsMailJob {
  case class Stats(sends: Int, opens: Int, clicks: Int)

  case class MandrillConfig(api: MandrillApi,
                            templateName: String)

  case class MixpanelConfig(api: MixpanelAPI,
                            propertyName: String,
                            eventNames: List[String])
}

class StatsMailJob(mandrill: MandrillConfig,
                   mixpanel: MixpanelConfig,
                   receivers: List[String]) extends PeriodicalJob[Period] {
  def id: JobId = JobId("email-template-stats")

  def run(config: Period)(implicit exc: ExecutionContext): Future[Unit] = {
    val mandrillStatsFuture = getHistory(config)
    val mixpanelStatsFuture = getMixpanelEventsStats
    mandrillStatsFuture.flatMap(mandrillStats =>
      mixpanelStatsFuture.flatMap(mixpanelStats =>
        sendEmails(config)(mandrillStats, mixpanelStats)
      ))
  }

  private def recipients = {
    val recs = new util.ArrayList[Recipient]()

    receivers.foreach(rec => {
      val recipient = new Recipient()
      recipient.setEmail(rec)
      recs.add(recipient)
    })
    recs
  }

  private def message(config: Period)(mandrillStats: Stats, mixpanelStats: String \/ List[EventHitsBySources]) = {
    val mandrillHTML = s"""<h3>Template "${mandrill.templateName}" stats
                          from ${config.from.toString("dd.MM.yyyy HH:mm")}
                          to ${config.to.toString("dd.MM.yyyy HH:mm")}</h3>
                          <p>Sent emails: ${mandrillStats.sends}</p>
                          <p>Opens: ${mandrillStats.opens}</p>
                          <p>Clicks: ${mandrillStats.clicks}</p>"""

    val mixpanelHTML = mixpanelStats match {
      case -\/(errorMessage) =>
        s"""<h3>Failed to obtain mixpanel stats. Reason: $errorMessage</h3>"""
      case \/-(stats) =>
        def sourceHTML(source: EventSource, hits: BigDecimal) =
          s"""<li>${source.name}: ${hits}</li>"""
        val eventsHTML = stats.map(s =>
          s"""
            <li>${s.event.name}:
              <ul>${s.hits.map{case (s,h) => sourceHTML(s,h)}.mkString("")}</ul>
            </li>
          """)

        s"""
          <h3>Mixpanel stats (last 24 hours)</h3>
          <p>
            Event hits by sources:
            <ul>
              ${eventsHTML.mkString("")}
            </ul>
          </p>
        """
    }

    val email = new MandrillMessage()
    email.setSubject( s"""Daily "${mandrill.templateName}" template stats.""")
    email.setHtml(mandrillHTML + mixpanelHTML)
    email.setAutoText(true)
    email.setFromEmail("mailer@catchapp.co")
    email.setFromName("Catchapp mailer")
    email.setTo(recipients)
    email.setPreserveRecipients(true)
    email
  }

  private def sendEmails(config: Period)(mandrillStats: Stats, mixpanelStats: String \/ List[EventHitsBySources])(implicit exc: ExecutionContext): Future[Unit] = Future {
    val statuses = mandrill.api.messages().send(message(config)(mandrillStats, mixpanelStats), false).toList
    statuses.foreach(status => if (status.getStatus() == "rejected" || status.getStatus() == "invalid")
      Logger.logger.error(s"Failed to send emails: ${status.getRejectReason()}")
    )
  }

  private def getHistory(config: Period)(implicit exc: ExecutionContext): Future[Stats] = Future {
    val rawHistory: List[MandrillTimeSeries] = mandrill.api.templates.timeSeries(mandrill.templateName).toList

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

  private def getMixpanelEventsStats(implicit exc: ExecutionContext): Future[String \/ List[EventHitsBySources]] =
    Future.sequence(mixpanel.eventNames.map(eventName => {
      val request = Request.EventsProperties(
        eventName,
        mixpanel.propertyName,
        AnalysisType.general,
        Interval(24, Interval.hour)
      )
      mixpanel.api.send(request, new MergedEventSourcesHitsParser(eventName)).map(_.leftMap(_.msg))
    })).map(_.sequenceU)
}
