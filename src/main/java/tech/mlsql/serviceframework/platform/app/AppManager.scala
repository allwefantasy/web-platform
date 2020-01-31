package tech.mlsql.serviceframework.platform.app

import tech.mlsql.serviceframework.platform.AppRuntimeStore

/**
 * 20/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
object AppManager {
  def call(appName: String, params: Map[Any, Any]): Any = {
    AppRuntimeStore.store.getApp(appName) match {
      case Some(item) =>
        val clzzName = item.className
        item.loader.loader.loadClass(clzzName).
          newInstance().asInstanceOf[CustomApp].run(params)
      case None => throw new RuntimeException(s"No app named ${appName}")
    }
  }


}
