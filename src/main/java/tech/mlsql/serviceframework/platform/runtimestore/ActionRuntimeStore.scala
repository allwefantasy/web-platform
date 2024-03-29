package tech.mlsql.serviceframework.platform.runtimestore

import tech.mlsql.serviceframework.platform.resilience.{RateLimiterFactory, RateLimiterHint}
import tech.mlsql.serviceframework.platform.{ActionItem, AppRuntimeStore, PluginLoader}

import scala.collection.JavaConverters._

trait ActionRuntimeStore {
  self: AppRuntimeStore =>
  def registerAction(name: String, className: String, loader: PluginLoader) = {
    store.write(ActionItem(name, className, loader))
  }

  def removeAction(name: String) = {
    store.delete(classOf[ActionItem], name)
  }

  def getActions() = {
    store.view(classOf[ActionItem]).iterator().asScala.toList
  }

  def getAction(name: String): Option[ActionItem] = {
    try {
      Some(store.read(classOf[ActionItem], name))
    } catch {
      case e: NoSuchElementException =>
        None
      case e: Exception => throw e
    }
  }
}
