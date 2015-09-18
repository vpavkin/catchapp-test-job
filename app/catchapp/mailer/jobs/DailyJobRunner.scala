package catchapp.mailer.jobs

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.reflect.ClassTag
import com.github.nscala_time.time.Imports.DateTime

class DailyJobRunner(actorSystem: ActorSystem,
                     jobStartTime: DateTime,
                     jobExecutionTimeout: FiniteDuration,
                     job: PeriodicalJob[Period]) extends JobRunner {

  import DailyJobActor._

  implicit val ec: ExecutionContext = actorSystem.dispatcher
  val actor = actorSystem.actorOf(
    Props(new DailyJobActor(jobStartTime, jobExecutionTimeout, job)),
    s"DailyJobActor-${job.id.value}"
  )

  def start(): Unit =
    actor ! Start
}
