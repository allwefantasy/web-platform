package tech.mlsql.serviceframework.platform.action.file

import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, TimeUnit}

import net.csdn.common.logging.Loggers
import org.apache.commons.io.FileUtils

class FileServer {

}

object FileServerDaemon {
  val logger = Loggers.getLogger(classOf[FileServer])
  val DEFAULT_TEMP_PATH = "/tmp/upload/"
  val executor = Executors.newSingleThreadScheduledExecutor()
  val uploadTime = new AtomicLong(0)

  def init = {
    if (uploadTime.getAndIncrement() == 0) {
      run
    }
  }

  def run = {
    executor.scheduleWithFixedDelay(new Runnable {
      override def run(): Unit = {
        val file = new File(DEFAULT_TEMP_PATH)
        file.listFiles().foreach { tempFile =>
          try {
            if (System.currentTimeMillis() - tempFile.lastModified() > 1000 * 60 * 120) {
              FileUtils.deleteQuietly(tempFile)
            }
          } catch {
            case e: Exception =>
              logger.error("Delete upload file fail", e)
          }

        }
      }
    }, 60, 60, TimeUnit.SECONDS)
  }
}
