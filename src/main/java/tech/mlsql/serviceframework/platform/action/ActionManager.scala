package tech.mlsql.serviceframework.platform.action

import io.github.resilience4j.ratelimiter.RateLimiter
import io.vavr.CheckedFunction0
import tech.mlsql.serviceframework.platform.AppRuntimeStore
import tech.mlsql.serviceframework.platform.resilience.RateLimiterFactory

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
object ActionManager {

  // Should not register by default
  //AppRuntimeStore.store.registerController("FileDownloadAction", classOf[FileDownloadAction].getName)
  //AppRuntimeStore.store.registerController("FileUploadAction", classOf[FileUploadAction].getName)

  def call(action: String, params: Map[String, String]): String = {
    AppRuntimeStore.store.getAction(action) match {
      case Some(item) =>
        val actionClassName = item.className
        val currentAction = item.loader.loader.loadClass(actionClassName).newInstance().asInstanceOf[CustomAction]

        if (RateLimiterFactory.ActionRateLimiterMapping.containsKey(action)) {
          val rateLimiter = RateLimiterFactory.ActionRateLimiterMapping.get(action)
          val flightsSupplier = RateLimiter.decorateCheckedSupplier(rateLimiter, new CheckedFunction0[String] {
            override def apply(): String = {
              currentAction.run(params)
            }
          })
          flightsSupplier.apply()
        } else {
          currentAction.run(params)
        }
      case None => throw new RuntimeException(s"No action named ${action}")
    }
  }
}


