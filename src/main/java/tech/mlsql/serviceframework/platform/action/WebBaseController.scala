package tech.mlsql.serviceframework.platform.action

import net.csdn.annotation.rest._
import net.csdn.common.exception.RenderFinish
import net.csdn.modules.http.ApplicationController
import net.csdn.modules.http.RestRequest.Method.{GET, POST}
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload
import tech.mlsql.common.utils.log.Logging
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.cleaner.RequestCleanerManager
import tech.mlsql.serviceframework.platform.exception.ExceptionRenderManager

import scala.collection.JavaConverters._

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class WebBaseController extends ApplicationController with Logging {
  @Action(
    summary = "used to execute MLSQL script", description = "async/sync supports"
  )
  @Parameters(Array(
    new Parameter(name = "action", required = false, description = "query|analyze; default is query", `type` = "string", allowEmptyValue = false)
  ))
  @Responses(Array(
    new ApiResponse(responseCode = "200", description = "", content = new Content(mediaType = "application/json",
      schema = new Schema(`type` = "string", format = """{}""", description = "")
    ))
  ))
  @At(path = Array("/run"), types = Array(GET, POST))
  def run = {

    ActionContext.setContext(buildActionContext)

    var outputResult: String = "[]"
    try {
      ActionContext.context().params.getOrElse("action", "default") match {
        case "default" =>
          outputResult = JSONTool.toJsonStr(Map("message" -> "Welcome to web-platform"))
        case action: String =>
          outputResult = ActionManager.call(action, ActionContext.context().params)
      }
      if (!ActionContext.context().stop) {
        render(outputResult)
      }

    } catch {
      case e: Exception if !e.isInstanceOf[RenderFinish] =>
        val msg = ExceptionRenderManager.call(e)
        render(500, msg.str.get)
    } finally {
      RequestCleanerManager.call()
    }
  }

  def buildActionContext = {
    //multipart/form-data; boundary=d680f034ceffcf7fd318d98048072b65
    val conentType = request.httpServletRequest().getHeader("Content-Type")
    val isMultiParetForm = conentType!=null && conentType.toLowerCase().stripMargin.startsWith("multipart/form-data;")
    if (isMultiParetForm) {
      val sfu = new ServletFileUpload(new DiskFileItemFactory())
      sfu.setHeaderEncoding("UTF-8")
      val formItems = sfu.parseRequest(request.httpServletRequest())
      val newParams = formItems.
        asScala.filter(f => f.isFormField).
        map(f => (f.getFieldName, new String(f.get(), "utf-8"))).toMap
      new ActionContext(HttpContext(request, restResponse), newParams, Map(ActionContext.Config.formItems -> formItems,
        ActionContext.Config.servletFileUpload -> sfu), false)
    } else {
      new ActionContext(HttpContext(request, restResponse), params().asScala.toMap, Map(), false)
    }

  }
}
