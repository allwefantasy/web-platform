package tech.mlsql.serviceframework.platform.action.file

import java.io.File
import java.nio.charset.Charset

import net.csdn.common.reflect.ReflectHelper
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.{HttpMultipartMode, MultipartEntityBuilder}
import tech.mlsql.common.utils.hdfs.HDFSOperator
import tech.mlsql.common.utils.log.Logging
import tech.mlsql.common.utils.path.PathFun

import scala.collection.mutable.ArrayBuffer

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class HttpFileClient extends Logging {

  def upload(uploadUrl: String, headers: Map[String, String], sourcePath: String): String = {
    val inputStream = HDFSOperator.readAsInputStream(sourcePath)
    val fileName = sourcePath.split(File.separator).last
    try {
      val entity = MultipartEntityBuilder.create.
        setMode(HttpMultipartMode.BROWSER_COMPATIBLE).
        setCharset(Charset.forName("utf-8")).
        addBinaryBody(fileName, inputStream, ContentType.MULTIPART_FORM_DATA, fileName).build
      logInfo(s"upload file ${sourcePath} to ${uploadUrl}")

      val uploader = Request.Post(uploadUrl).connectTimeout(60 * 1000)
        .socketTimeout(10 * 60 * 1000)

      headers.foreach { header =>
        uploader.addHeader(header._1, header._2)
      }

      uploader.body(entity).execute().returnContent().asString()

    } finally {
      inputStream.close()
    }
  }

  def download(downloadUrl: String, saveLocation: String) = {
    val response = Request.Get(downloadUrl)
      .connectTimeout(60 * 1000)
      .socketTimeout(10 * 60 * 1000)
      .execute()
    // Since response always consume the inputstream and return new stream, this will cost too much memory.
    val stream = ReflectHelper.field(response, "response").asInstanceOf[HttpResponse].getEntity.getContent
    val tarIS = new TarArchiveInputStream(stream)

    var downloadResultRes = ArrayBuffer[DownloadResult]()
    try {
      var entry = tarIS.getNextEntry
      while (entry != null) {
        if (tarIS.canReadEntryData(entry)) {
          if (!entry.isDirectory) {
            val dir = entry.getName.split("/").filterNot(f => f.isEmpty).dropRight(1).mkString("/")
            downloadResultRes += DownloadResult(PathFun(saveLocation).add(dir).add(entry.getName.split("/").last).toPath)
            logInfo(s"extracting ${downloadResultRes.last.path}")
            HDFSOperator.saveStream(PathFun(saveLocation).add(dir).toPath, entry.getName.split("/").last, tarIS)
          }
          entry = tarIS.getNextEntry
        }
      }
    } finally {
      tarIS.close()
      stream.close()
    }
    downloadResultRes
  }
}

case class DownloadResult(path: String)
