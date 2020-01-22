package tech.mlsql.serviceframework.platform.cleaner

import tech.mlsql.serviceframework.platform.AppRuntimeStore


object RequestCleanerManager {

  def call() = {
    AppRuntimeStore.store.getRequestCleaners().foreach { cleaner =>
      Class.forName(cleaner.customClassItem.className, true, cleaner.customClassItem.loader.loader).newInstance().asInstanceOf[RequestCleaner].call()
    }
  }
}
