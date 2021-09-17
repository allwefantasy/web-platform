package tech.mlsql.serviceframework.platform.action.file.action

import net.csdn.common.jline.ANSI.Renderer.RenderException
import tech.mlsql.common.utils.Md5
import tech.mlsql.common.utils.log.Logging
import tech.mlsql.common.utils.path.PathFun
import tech.mlsql.serviceframework.platform.action.file.{DownloadRunner, FileServerDaemon}
import tech.mlsql.serviceframework.platform.action.{ActionContext, CustomAction, HttpContext}

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class FileDownloadAction extends CustomAction with Logging {

  def run(params: Map[String, String]): String = {

    // local/hdfs
    params.getOrElse("downloadType", "local") match {
      case "local" =>
        localDownLoad(params)
        throw new RenderException("")
      case "hdfs" =>
        hdfsDownload(params)
        throw new RenderException("")
    }

  }

  def hdfsDownload(params: Map[String, String]) = {
    val HttpContext(_, restResponse) = ActionContext.context().httpContext
    val filename = params.getOrElse("fileName", System.currentTimeMillis() + "")
    params.getOrElse("fileType", "raw") match {
      case "tar" =>
        restResponse.httpServletResponse().setContentType("application/octet-stream")
        restResponse.httpServletResponse().addHeader("Content-Disposition", "attachment;filename=" + new String((filename + ".tar").getBytes))
        restResponse.httpServletResponse().addHeader("Transfer-Encoding", "chunked")
        DownloadRunner.getHDFSTarFileByPath(restResponse.httpServletResponse(), params("paths")) match {
          case 200 => render(restResponse, "success")
          case 400 => render(restResponse, 400, "download fail")
          case 500 => render(restResponse, 500, "server error")
        }
      case "raw" =>
        restResponse.httpServletResponse().setContentType("application/octet-stream")
        restResponse.httpServletResponse().addHeader("Content-Disposition", "attachment;filename=" + new String((filename + "." + params("file_suffix")).getBytes))
        restResponse.httpServletResponse().addHeader("Transfer-Encoding", "chunked")
        DownloadRunner.getHDFSRawFileByPath(restResponse.httpServletResponse(), params("paths"), params.getOrElse("pos", "0").toLong) match {
          case 200 => render(restResponse, "success")
          case 400 => render(restResponse, 400, "download fail")
          case 500 => render(restResponse, 500, "server error")
        }


    }
  }

  def localDownLoad(params: Map[String, String]) = {
    val HttpContext(request, response) = ActionContext.context().httpContext

    if (!params.contains("fileName")) {
      render(response, 404, "fileName required")
    }

    val userName = if (params.contains("userName")) {
      Md5.md5Hash(params("userName"))
    } else ""
    var targetFilePath = PathFun(FileServerDaemon.DEFAULT_TEMP_PATH).add(userName).add(params("fileName")).toPath
    if (params("fileName").startsWith("public/")) {
      targetFilePath = "/data/mlsql/data/" + params("fileName")
    }
    logInfo(s"Write ${targetFilePath} to response")
    try {
      if (params("fileName").endsWith(".tar")) {
        DownloadRunner.getTarFileByTarFile(response.httpServletResponse(), targetFilePath)
      } else {
        DownloadRunner.getTarFileByPath(response.httpServletResponse(), targetFilePath)
      }

    } catch {
      case e: Exception =>
        logError("download fail", e)
    }
  }

}
