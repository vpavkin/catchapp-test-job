package catchapp.mailer.jobs

import scala.concurrent.{ExecutionContext, Future}

trait Job[C] {
  def id: JobId
  def run(runConfig: C)(implicit exc: ExecutionContext): Future[Unit]
}


