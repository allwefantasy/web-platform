package tech.mlsql.serviceframework.platform.plugin

import tech.mlsql.serviceframework.platform.cleaner.ActionContextCleaner
import tech.mlsql.serviceframework.platform.{Plugin, PluginItem, PluginType}

/**
 * 21/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class DefaultPlugin extends Plugin {
  override def entries: List[PluginItem] = {
    List(PluginItem("ActionContextCleaner", classOf[ActionContextCleaner].getName, PluginType.cleaner, None))
  }
}
