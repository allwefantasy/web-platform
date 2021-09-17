package tech.mlsql.serviceframework.platform.action.file.action

import java.io.File

import org.apache.commons.fileupload.disk.DiskFileItemFactory
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.io.FileUtils
import tech.mlsql.common.utils.Md5
import tech.mlsql.common.utils.log.Logging
import tech.mlsql.common.utils.path.PathFun
import tech.mlsql.serviceframework.platform.action.file.FileServerDaemon
import tech.mlsql.serviceframework.platform.action.{ActionContext, CustomAction, HttpContext}

import scala.collection.JavaConverters._

class FileUploadAction extends CustomAction with Logging {
  override def run(params: Map[String, String]): String = {
    val HttpContext(request, response) = ActionContext.context().httpContext

    val totalSizeLimit = params.getOrElse("totalSizeLimit", "-1").toLong
    val userName = if (params.contains("userName")) {
      Md5.md5Hash(params("userName"))
    } else ""


    FileServerDaemon.init
    val sfu = new ServletFileUpload(new DiskFileItemFactory())
    sfu.setHeaderEncoding("UTF-8")
    val items = sfu.parseRequest(request.httpServletRequest())

    val homeDir = new File(PathFun(FileServerDaemon.DEFAULT_TEMP_PATH).add(userName).toPath)
    var finalDir = ""
    if (homeDir.exists()) {
      val totalSize = FileUtils.sizeOfDirectory(homeDir)
      if (totalSizeLimit != -1 && totalSize > totalSizeLimit) {
        render(response, 400, s"You have no enough space. The limit is ${totalSizeLimit} bytes")
      }
    }

    items.asScala.filterNot(f => f.isFormField).headOption match {
      case Some(f) =>
        val prefix = PathFun(FileServerDaemon.DEFAULT_TEMP_PATH).add(userName).toPath
        val itemPath = f.getFieldName
        val chunks = itemPath.split("/").filterNot(f => f.isEmpty)

        if (chunks.filter(f => (f.trim == "." || f.trim == "..")).length != 0) {
          render(response, 400, "file path is not correct")
        }

        if (chunks.size > 0) {
          val file = new File(prefix + "/" + chunks.head)
          FileUtils.deleteQuietly(file)
        } else {
          FileUtils.deleteQuietly(new File(prefix + "/" + itemPath))
        }
      case None =>
    }

    try {
      items.asScala.filterNot(f => f.isFormField).map {
        item =>
          val fileContent = item.getInputStream()
          val tempFilePath = PathFun(FileServerDaemon.DEFAULT_TEMP_PATH).add(userName).add(item.getFieldName).toPath
          val dir = new File(tempFilePath.split("/").dropRight(1).mkString("/"))
          if (!dir.exists()) {
            dir.mkdirs()
          }
          val targetPath = new File(tempFilePath)

          if (tempFilePath.substring(homeDir.getPath.length).stripPrefix("/").stripSuffix("/").split("/").length >= 2) {
            finalDir = dir.getPath.substring(homeDir.getPath.length())
          } else {
            finalDir = tempFilePath.substring(homeDir.getPath.length())
          }
          //upload.setSizeMax(yourMaxRequestSize);
          logInfo(s"upload to ${targetPath.getPath}")
          FileUtils.copyInputStreamToFile(fileContent, targetPath)
          fileContent.close()
      }
    } catch {
      case e: Exception =>
        logInfo("upload fail ", e)
        render(response, 500, s"upload fail,check master log ${e.getMessage}")

    }
    ""
  }
}
