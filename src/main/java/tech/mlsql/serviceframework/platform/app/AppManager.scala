package tech.mlsql.serviceframework.platform.app

import tech.mlsql.serviceframework.platform.AppRuntimeStore
import tech.mlsql.serviceframework.platform.action.CustomAction

/**
 * 20/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
object AppManager {
  def call(appName: String, params: Map[String, String]): String = {
    AppRuntimeStore.store.getApp(appName) match {
      case Some(item) =>
        val clzzName = item.className
        Class.forName(clzzName, true, AppRuntimeStore.store.getLoader(appName).loader.loader).
          newInstance().asInstanceOf[CustomAction].run(params)
      case None => throw new RuntimeException(s"No app named ${appName}")
    }
  }
}
