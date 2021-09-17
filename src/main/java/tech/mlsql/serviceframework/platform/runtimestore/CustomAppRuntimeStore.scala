package tech.mlsql.serviceframework.platform.runtimestore

import tech.mlsql.serviceframework.platform.app.StartupPhase
import tech.mlsql.serviceframework.platform.{AppItem, AppRuntimeStore, PluginLoader}

import scala.collection.JavaConverters._

trait CustomAppRuntimeStore {
  self: AppRuntimeStore =>
  def registerApp(name: String, className: String, loader: PluginLoader, phase: Option[StartupPhase]) = {
    store.write(AppItem(name, className, loader, phase))
  }

  def removeApp(name: String) = {
    store.delete(classOf[AppItem], name)
  }

  def getApps(): List[AppItem] = {
    store.view(classOf[AppItem]).iterator().asScala.toList
  }

  def getApp(name: String): Option[AppItem] = {
    try {
      Some(store.read(classOf[AppItem], name))
    } catch {
      case e: NoSuchElementException =>
        None
      case e: Exception => throw e
    }

  }
}
