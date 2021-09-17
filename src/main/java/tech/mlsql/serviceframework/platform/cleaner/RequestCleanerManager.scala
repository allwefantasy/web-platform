package tech.mlsql.serviceframework.platform.cleaner

import tech.mlsql.serviceframework.platform.AppRuntimeStore


object RequestCleanerManager {

  def call() = {
    AppRuntimeStore.store.getRequestCleaners().foreach { cleaner =>
      cleaner.customClassItem.loader.loader.loadClass(cleaner.customClassItem.className).newInstance().asInstanceOf[RequestCleaner].call()
    }
  }
}
