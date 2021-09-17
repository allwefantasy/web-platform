package tech.mlsql.serviceframework.platform.action

import net.csdn.common.exception.RenderFinish
import net.csdn.modules.http.{RestRequest, RestResponse}


trait RenderFunctions {
  def render(restResponse: RestResponse, content: String): Unit = {
    restResponse.originContent(content)
    restResponse.write(content)
    throw new RenderFinish
  }

  //渲染输出
  def render(restResponse: RestResponse, status: Int, content: String): Unit = {
    restResponse.originContent(content)
    restResponse.write(status, content)
    throw new RenderFinish
  }
}

trait CustomAction extends RenderFunctions {
  def run(params: Map[String, String]): String
}


object ActionContext {
  private[this] val actionContext: ThreadLocal[ActionContext] = new ThreadLocal[ActionContext]()

  def context(): ActionContext = actionContext.get

  def setContext(ec: ActionContext): Unit = actionContext.set(ec)

  def setContextIfNotPresent(ec: ActionContext): Unit = {
    if (ActionContext.context() == null) {
      actionContext.set(ec)
    }
  }

  def unset = {
    val c = actionContext.get()
    actionContext.remove()
    c
  }

  object Config {
    val servletFileUpload = "servletFileUpload"
    val formItems = "formItems"
  }

}

case class ActionContext(httpContext: HttpContext,
                         params: Map[String, String],
                         others: Map[Any, Any],
                         var stop: Boolean)

case class HttpContext(request: RestRequest,
                       response: RestResponse)
