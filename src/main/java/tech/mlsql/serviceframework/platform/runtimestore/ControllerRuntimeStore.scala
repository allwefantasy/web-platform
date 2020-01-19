package tech.mlsql.serviceframework.platform.runtimestore

import tech.mlsql.serviceframework.platform.{AppRuntimeStore, CustomClassItem, CustomClassItemWrapper}

trait ControllerRuntimeStore {
  self: AppRuntimeStore =>
  def registerController(name: String, className: String) = {
    store.write(CustomClassItemWrapper(CustomClassItem(name, className)))
  }

  def removeController(name: String) = {
    store.delete(classOf[CustomClassItemWrapper], name)
  }

  def getController(name: String): Option[CustomClassItemWrapper] = {
    try {
      Some(store.read(classOf[CustomClassItemWrapper], name))
    } catch {
      case e: NoSuchElementException =>
        None
      case e: Exception => throw e
    }

  }
}
