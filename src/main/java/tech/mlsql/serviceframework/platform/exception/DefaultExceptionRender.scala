package tech.mlsql.serviceframework.platform.exception

import tech.mlsql.common.utils.log.Logging

import scala.collection.mutable.ArrayBuffer

class DefaultExceptionRender extends ExceptionRender with Logging {
  override def format(e: Exception): String = {
    e.printStackTrace()
    val msgBuffer = ArrayBuffer[String]()
    ExceptionUtils.format_full_exception(msgBuffer, e)
    e.getMessage + "\n" + msgBuffer.mkString("\n")
  }

  override def is_match(e: Exception): Boolean = true
}

object ExceptionUtils {

  def format(msg: String, skipPrefix: Boolean = false) = {
    msg
  }

  def format_exception(e: Exception) = {
    (e.toString.split("\n") ++ e.getStackTrace.map(f => f.toString)).map(f => format(f)).toSeq.mkString("\n")
  }

  def format_throwable(e: Throwable, skipPrefix: Boolean = false) = {
    (e.toString.split("\n") ++ e.getStackTrace.map(f => f.toString)).map(f => format(f, skipPrefix)).toSeq.mkString("\n")
  }

  def format_cause(e: Exception) = {
    var cause = e.asInstanceOf[Throwable]
    while (cause.getCause != null) {
      cause = cause.getCause
    }
    format_throwable(cause)
  }

  def format_full_exception(buffer: ArrayBuffer[String], e: Exception, skipPrefix: Boolean = true) = {
    var cause = e.asInstanceOf[Throwable]
    buffer += format_throwable(cause, skipPrefix)
    while (cause.getCause != null) {
      cause = cause.getCause
      buffer += "caused by：\n" + format_throwable(cause, skipPrefix)
    }

  }
}
