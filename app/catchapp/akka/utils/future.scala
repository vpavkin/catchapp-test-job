package catchapp.akka.utils

import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.after

import scala.concurrent.{ExecutionContext, Future}

object future {

  implicit class FutureTimeout[A](f: Future[A]) {
    def withTimeout(timeout: Timeout, system: ActorSystem)(implicit ec: ExecutionContext): Future[A] = {
      Future.firstCompletedOf(Seq(f, after(timeout.duration, system.scheduler)(Future.failed(new TimeoutException))))
    }
  }
}
