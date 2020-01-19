package tech.mlsql.serviceframework.platform.runtimestore

import tech.mlsql.serviceframework.platform.{AppRuntimeStore, CustomClassItem, ExceptionRenderItemWrapper}

trait ExceptionRenderRuntimeStore {
  self: AppRuntimeStore =>
  def registerExceptionRender(name: String, className: String) = {
    store.write(ExceptionRenderItemWrapper(CustomClassItem(name, className)))
  }

  def removeExceptionRender(name: String) = {
    store.delete(classOf[ExceptionRenderItemWrapper], name)
  }

  def getExceptionRenders(): List[ExceptionRenderItemWrapper] = {
    try {
      import scala.collection.JavaConverters._
      val items = store.view(classOf[ExceptionRenderItemWrapper]).iterator().asScala.toList
      items
    } catch {
      case e: NoSuchElementException =>
        List()
      case e: Exception => throw e
    }

  }

}
