package catchapp.mailer.jobs

import org.joda.time.DateTime

trait Period {
  def from: DateTime
  def to: DateTime
}

trait PeriodicalJob[C <: Period] extends Job[C]


