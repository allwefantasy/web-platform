package tech.mlsql.serviceframework.platform.runtimestore

import tech.mlsql.serviceframework.platform.{ActionItem, AppItem, AppRuntimeStore}

import scala.collection.JavaConverters._

trait ActionRuntimeStore {
  self: AppRuntimeStore =>
  def registerAction(name: String, className: String) = {
    store.write(ActionItem(name, className))
  }

  def removeAction(name: String) = {
    store.delete(classOf[ActionItem], name)
  }

  def getActions() = {
    store.view(classOf[AppItem]).iterator().asScala.toList
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
