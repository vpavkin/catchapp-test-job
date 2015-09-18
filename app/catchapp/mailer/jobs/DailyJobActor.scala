package catchapp.mailer.jobs

import akka.actor.{Actor, ActorLogging, Stash}
import akka.util.Timeout
import catchapp.akka.utils.future
import com.github.nscala_time.time.Imports.{Duration => _, _}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import future._

object DailyJobActor {
  case object Start
  case object RunJob
  case object JobComplete
}

class DailyJobActor(jobStartTime: DateTime,
                    jobExecutionTimeout: FiniteDuration,
                    job: PeriodicalJob[Period]) extends Actor with ActorLogging with Stash {

  import DailyJobActor._

  implicit val ec: ExecutionContext = context.system.dispatcher

  override def postRestart(reason: Throwable): Unit = {
    log.error(reason, s"DailyJobActor for job '${job.id.value}' did restart")
    self ! Start
    super.postRestart(reason)
  }

  private def timeUntilNextJob: FiniteDuration = {
    val runDate = if (DateTime.now.getSecondOfDay > jobStartTime.getSecondOfDay)
      DateTime.tomorrow
    else
      DateTime.now
    val runTime = runDate
      .hour(jobStartTime.getHourOfDay)
      .minute(jobStartTime.getMinuteOfHour)
      .second(jobStartTime.getSecondOfMinute)
    val res = Duration(runTime.getMillis - DateTime.now.getMillis, MILLISECONDS)
    log.info(s"Minutes until next delivery: ${res.toMinutes}")
    res
  }

  override def preStart(): Unit = {
    log.info(s"DailyJobActor for job '${job.id}' started")
    context.system.scheduler.scheduleOnce(timeUntilNextJob, self, RunJob)
  }

  override def receive = initial

  def initial: Receive = {
    case Start =>
      unstashAll()
      context.become(idle)
    case _ =>
      stash()
  }

  def idle: Receive = {
    case RunJob =>
      runJob()
      context.become(running)
  }

  def running: Receive = {
    case JobComplete =>
      context.system.scheduler.scheduleOnce(timeUntilNextJob, self, RunJob)
      context.become(idle)
      unstashAll()
    case _ =>
      stash()
  }

  def runJob() = {
    val f = job.run(new Period {
      def from: DateTime = DateTime.yesterday
      def to: DateTime = DateTime.now
    })
      .withTimeout(Timeout(jobExecutionTimeout), context.system)

    f.onComplete({
      case Success(_) =>
        log.info(s"Job '${job.id.value}' is complete")
        self ! JobComplete
      case Failure(e) =>
        log.error(e, s"Failed to complete job '${job.id.value}'. Reason: $e")
        self ! JobComplete
    })
    f
  }
}
