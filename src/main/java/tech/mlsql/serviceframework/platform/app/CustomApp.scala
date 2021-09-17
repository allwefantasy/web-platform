package tech.mlsql.serviceframework.platform.app

/**
 * 21/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
trait CustomApp {
  def run(params: Map[Any, Any]): Any

  def phase: StartupPhase
}

sealed abstract class StartupPhase
(
  val name: String
)

case object BeforeHTTPPhase extends StartupPhase("before_http")

case object AfterHTTPPhase extends StartupPhase("after_http")
