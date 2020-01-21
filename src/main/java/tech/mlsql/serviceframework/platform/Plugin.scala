package tech.mlsql.serviceframework.platform

import java.net.{URL, URLClassLoader}

import tech.mlsql.serviceframework.platform.app.StartupPhase

/**
 * 20/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
trait Plugin {
  def entries: List[PluginItem]
}

case class PluginItem(name: String, clzzName: String, pluginType: String, phase: Option[StartupPhase] = None)

object PluginType {
  val action = "action"
  val app = "app"
  val exception = "exception"
  val cleaner = "cleaner"
}

case class PluginLoader(loader: ClassLoader, plugin: Plugin)

object PluginLoader {
  def load(url: String, pluginClassName: String) = {
    val loader = new URLClassLoader(Array(new URL(url)))
    val plugin = loader.loadClass(pluginClassName).newInstance().asInstanceOf[Plugin]

    val pluginLoader = PluginLoader(loader, plugin)
    AppRuntimeStore.store.registerClzzLoader(pluginClassName, pluginLoader)

    plugin.entries.foreach { item =>
      item.pluginType match {
        case PluginType.action =>
          AppRuntimeStore.store.registerAction(item.name, item.clzzName)
        case PluginType.app =>
          AppRuntimeStore.store.registerApp(item.name, item.clzzName,item.phase)
        case PluginType.exception =>
          AppRuntimeStore.store.registerExceptionRender(item.name, item.clzzName)
        case PluginType.cleaner =>
          AppRuntimeStore.store.registerRequestCleaner(item.name, item.clzzName)
      }

    }

  }
}
