package tech.mlsql.serviceframework.platform.action

import tech.mlsql.serviceframework.platform.AppRuntimeStore

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
object ActionManager {

  // Should not register by default
  //AppRuntimeStore.store.registerController("FileDownloadAction", classOf[FileDownloadAction].getName)
  //AppRuntimeStore.store.registerController("FileUploadAction", classOf[FileUploadAction].getName)

  def call(action: String, params: Map[String, String]): String = {
    AppRuntimeStore.store.getAction(action) match {
      case Some(item) =>
        val actionClassName = item.className
        item.loader.loader.loadClass(actionClassName).newInstance().asInstanceOf[CustomAction].run(params)
      case None => throw new RuntimeException(s"No action named ${action}")
    }
  }
}


