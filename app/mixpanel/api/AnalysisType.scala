package mixpanel.api

sealed trait AnalysisType

object AnalysisType {
  case object general extends AnalysisType
  case object unique extends AnalysisType
  case object average extends AnalysisType
}
