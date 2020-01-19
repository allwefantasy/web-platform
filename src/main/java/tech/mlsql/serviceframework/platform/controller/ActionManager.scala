package tech.mlsql.serviceframework.platform.controller

import tech.mlsql.serviceframework.platform.AppRuntimeStore

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
object ActionManager {

  // Should not register by default
  //AppRuntimeStore.store.registerController("FileDownloadAction", classOf[FileDownloadAction].getName)
  //AppRuntimeStore.store.registerController("FileUploadAction", classOf[FileUploadAction].getName)

  def call(action: String, params: Map[String, String]): String = {
    AppRuntimeStore.store.getController(action) match {
      case Some(item) =>
        Class.forName(item.customClassItem.className).
          newInstance().asInstanceOf[CustomAction].run(params)
      case None => throw new RuntimeException(s"No action named ${action}")
    }
  }
}
