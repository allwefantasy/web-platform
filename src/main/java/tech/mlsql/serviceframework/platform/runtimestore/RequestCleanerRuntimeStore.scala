package tech.mlsql.serviceframework.platform.runtimestore

import tech.mlsql.serviceframework.platform.{AppRuntimeStore, CustomClassItem, PluginLoader, RequestCleanerItemWrapper}

trait RequestCleanerRuntimeStore {
  self: AppRuntimeStore =>
  def registerRequestCleaner(name: String, className: String, loader: PluginLoader) = {
    store.write(RequestCleanerItemWrapper(CustomClassItem(name, className, loader)))
  }

  def removeRequestCleaner(name: String) = {
    store.delete(classOf[RequestCleanerItemWrapper], name)
  }

  def getRequestCleaners(): List[RequestCleanerItemWrapper] = {
    try {
      import scala.collection.JavaConverters._
      val items = store.view(classOf[RequestCleanerItemWrapper]).iterator().asScala.toList
      items
    } catch {
      case e: NoSuchElementException =>
        List()
      case e: Exception => throw e
    }

  }

}
