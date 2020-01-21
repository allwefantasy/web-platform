package tech.mlsql.serviceframework.platform.loader

import tech.mlsql.serviceframework.platform.{AppRuntimeStore, ClzzLoaderItem, PluginLoader}

import scala.collection.JavaConverters._

/**
 * 20/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
trait ClzzLoaderRuntimeStore {
  self: AppRuntimeStore =>
  def registerClzzLoader(name: String, loader: PluginLoader) = {
    store.write(ClzzLoaderItem(name, loader))
  }

  def clzzLoaders = {
    store.view(classOf[ClzzLoaderItem]).iterator().asScala.toList
  }

  def getLoader(name: String) = {
    store.read(classOf[ClzzLoaderItem], name)
  }
}
