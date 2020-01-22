package tech.mlsql.serviceframework.platform.exception

import tech.mlsql.serviceframework.platform.AppRuntimeStore

object ExceptionRenderManager {

  def call(e: Exception): ExceptionResult = {
    var meet = false
    var target: ExceptionResult = null
    AppRuntimeStore.store.getExceptionRenders().foreach { exRender =>
      if (!meet) {
        val item = Class.forName(exRender.customClassItem.className, true, exRender.customClassItem.loader.loader).newInstance().asInstanceOf[ExceptionRender].call(e: Exception)
        if (item.str.isDefined) {
          meet = true
          target = item
        }
      }
    }
    if (meet) target else new DefaultExceptionRender().call(e)
  }
}

trait ExceptionRender {
  def format(e: Exception): String

  def is_match(e: Exception): Boolean

  final def call(e: Exception): ExceptionResult = {
    try {
      if (is_match(e)) {
        ExceptionResult(e, Option(format(e)))
      } else {
        ExceptionResult(e, None)
      }

    } catch {
      case e1: Exception => e1.printStackTrace()
        ExceptionResult(e, None)
    }

  }
}

case class ExceptionResult(e: Exception, str: Option[String])
