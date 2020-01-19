package tech.mlsql.serviceframework.platform.controller

import net.csdn.annotation.rest._
import net.csdn.modules.http.ApplicationController
import net.csdn.modules.http.RestRequest.Method.{GET, POST}
import tech.mlsql.common.utils.log.Logging
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
    new Parameter(name = "sql", required = true, description = "script content", `type` = "string", allowEmptyValue = false),
    new Parameter(name = "owner", required = false,
      description = "the user who execute this API and also will be used in MLSQL script automatically. " +
        "default: admin. Please set this owner properly.",
      `type` = "boolean", allowEmptyValue = false),
    new Parameter(name = "jobType", required = false, description = "script|stream|sql; default is script", `type` = "string", allowEmptyValue = false),
    new Parameter(name = "action", required = false, description = "query|analyze; default is query", `type` = "string", allowEmptyValue = false),
    new Parameter(name = "jobName", required = false, description = "give the job you submit a name. uuid is ok.", `type` = "string", allowEmptyValue = false),
    new Parameter(name = "timeout", required = false, description = "set timeout value for your job. default is -1 which means it is never timeout. millisecond", `type` = "int", allowEmptyValue = false),
    new Parameter(name = "silence", required = false, description = "the last sql in the script will return nothing. default: false", `type` = "boolean", allowEmptyValue = false),
    new Parameter(name = "sessionPerUser", required = false, description = "If set true, the owner will have their own session otherwise share the same. default: false", `type` = "boolean", allowEmptyValue = false),
    new Parameter(name = "sessionPerRequest", required = false, description = "by default false", `type` = "boolean", allowEmptyValue = false),
    new Parameter(name = "async", required = false, description = "If set true ,please also provide a callback url use `callback` parameter and the job will run in background and the API will return.  default: false", `type` = "boolean", allowEmptyValue = false),
    new Parameter(name = "callback", required = false, description = "Used when async is set true. callback is a url. default: false", `type` = "string", allowEmptyValue = false),
    new Parameter(name = "skipInclude", required = false, description = "disable include statement. default: false", `type` = "boolean", allowEmptyValue = false),
    new Parameter(name = "skipAuth", required = false, description = "disable table authorize . default: true", `type` = "boolean", allowEmptyValue = false),
    new Parameter(name = "skipGrammarValidate", required = false, description = "validate mlsql grammar. default: true", `type` = "boolean", allowEmptyValue = false)
  ))
  @Responses(Array(
    new ApiResponse(responseCode = "200", description = "", content = new Content(mediaType = "application/json",
      schema = new Schema(`type` = "string", format = """{}""", description = "")
    ))
  ))
  @At(path = Array("/run/script"), types = Array(GET, POST))
  def runScript = {
    var outputResult: String = "[]"
    try {
      params.getOrDefault("action", "default") match {
        case "default" =>
        case action: String =>
          outputResult = ActionManager.call(action, params().asScala.toMap)
      }

    } catch {
      case e: Exception =>
        val msg = ExceptionRenderManager.call(e)
        render(500, msg.str.get)
    } finally {
      RequestCleanerManager.call()
    }
    if (!ActionContext.context().stop) {
      render(outputResult)
    }

  }

}
