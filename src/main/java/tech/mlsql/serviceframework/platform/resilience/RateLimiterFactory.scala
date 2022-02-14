package tech.mlsql.serviceframework.platform.resilience

import io.github.resilience4j.ratelimiter.{RateLimiter, RateLimiterConfig, RateLimiterRegistry}
import net.csdn.ServiceFramwork
import net.csdn.common.settings.Settings
import tech.mlsql.common.utils.distribute.socket.server.JavaUtils

import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

/**
 * 13/2/2022 WilliamZhu(allwefantasy@gmail.com)
 */
object RateLimiterFactory {

  def settings = ServiceFramwork.injector.getInstance(classOf[Settings])

  lazy val ActionRateLimiterMapping = new ConcurrentHashMap[String, RateLimiter]()

  lazy val limiterInstances: Map[String, RateLimiterRegistry] = {
    val instances = settings.getGroups("ratelimiter.instances.")
    instances.asScala.map { item =>
      val limitForPeriod = item._2.get("limitForPeriod")
      val limitRefreshPeriod = item._2.get("limitRefreshPeriod")
      val timeoutDuration = item._2.get("timeoutDuration")

      val config = RateLimiterConfig.custom()
        .limitForPeriod(limitForPeriod.toInt)
        .limitRefreshPeriod(Duration.ofSeconds(JavaUtils.timeStringAsSec(limitRefreshPeriod)))
        .timeoutDuration(Duration.ofSeconds(JavaUtils.timeStringAsSec(timeoutDuration)))
        .build()
      val registry = RateLimiterRegistry.of(config)

      (item._1, registry)
    }.toMap
  }
}
