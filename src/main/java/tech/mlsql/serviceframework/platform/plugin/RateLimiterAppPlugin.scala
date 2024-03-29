package tech.mlsql.serviceframework.platform.plugin

import tech.mlsql.serviceframework.platform.app.{AfterHTTPPhase, CustomApp, StartupPhase}
import tech.mlsql.serviceframework.platform.resilience.{RateLimiterFactory, RateLimiterHint}
import tech.mlsql.serviceframework.platform.{AppRuntimeStore, Plugin, PluginItem, PluginType}

/**
 * 14/2/2022 WilliamZhu(allwefantasy@gmail.com)
 */
class RateLimiterAppPlugin extends CustomApp {
  override def run(params: Map[Any, Any]): Any = {

    AppRuntimeStore.store.getActions().foreach { item =>
      val tempClzz = item.loader.loader.loadClass(item.className)

      //configure RateLimiter
      if (tempClzz.isAnnotationPresent(classOf[RateLimiterHint])) {
        val rateLimiterInfo = tempClzz.getAnnotation(classOf[RateLimiterHint])
        assert(rateLimiterInfo.names().length == 1, "RateLimiter only support one in this version")
        val rateLimiterName = rateLimiterInfo.names()(0)

        if (!RateLimiterFactory.limiterInstances.contains(rateLimiterName)) {
          throw new RuntimeException(s"Ratelimiter [${rateLimiterName}] is not configured in application.yml")
        }
        RateLimiterFactory.ActionRateLimiterMapping.put(item.name,
          RateLimiterFactory.limiterInstances(rateLimiterName).rateLimiter(item.name))
      }
    }
  }

  override def phase: StartupPhase = AfterHTTPPhase
}

class RateLimiterAppPluginDesc extends Plugin {
  override def entries: List[PluginItem] = List(
    PluginItem("rateLimiterApp", classOf[RateLimiterAppPlugin].getName, PluginType.app, Option(AfterHTTPPhase))
  )
}
