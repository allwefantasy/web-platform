package tech.mlsql.serviceframework.platform.cleaner

import tech.mlsql.serviceframework.platform.AppRuntimeStore


object RequestCleanerManager {

  AppRuntimeStore.store.registerRequestCleaner("ActionContextCleaner", classOf[ActionContextCleaner].getName)

  def call() = {
    AppRuntimeStore.store.getRequestCleaners().foreach { cleaner =>
      Class.forName(cleaner.customClassItem.className).newInstance().asInstanceOf[RequestCleaner].call()
    }
  }
}
