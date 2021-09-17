package tech.mlsql.serviceframework.platform

import java.io.File
import java.net.URL

import tech.mlsql.serviceframework.platform.app.StartupPhase
import tech.mlsql.serviceframework.platform.cleaner.ActionContextCleaner
import tech.mlsql.serviceframework.platform.plugin.{DefaultPlugin, RuntimePluginLoader}

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
  def load(urls: Array[String], pluginClassName: String) = {
    def toUrl(url: String) = if (url.toLowerCase().startsWith("http://")) {
      new URL(url)
    } else if (url.toLowerCase().startsWith("https://")) {
      new URL(url)
    } else {
      new File(url).toURI.toURL
    }

    val loader = new RuntimePluginLoader(urls.map(toUrl(_)))
    val plugin = loader.loadClass(pluginClassName).newInstance().asInstanceOf[Plugin]

    val pluginLoader = PluginLoader(loader, plugin)
    //AppRuntimeStore.store.registerClzzLoader(pluginClassName, pluginLoader)

    plugin.entries.foreach { item =>
      item.pluginType match {
        case PluginType.action =>
          AppRuntimeStore.store.registerAction(item.name, item.clzzName, pluginLoader)
        case PluginType.app =>
          AppRuntimeStore.store.registerApp(item.name, item.clzzName, pluginLoader, item.phase)
        case PluginType.exception =>
          AppRuntimeStore.store.registerExceptionRender(item.name, item.clzzName, pluginLoader)
        case PluginType.cleaner =>
          AppRuntimeStore.store.registerRequestCleaner(item.name, item.clzzName, pluginLoader)
      }

    }

    val defaultCleanerName = "ActionContextCleaner"
    if (!AppRuntimeStore.store.getAction(defaultCleanerName).isDefined) {
      val defaultPlugin = new DefaultPlugin()
      AppRuntimeStore.store.registerRequestCleaner(defaultCleanerName, classOf[ActionContextCleaner].getName,
        PluginLoader(Thread.currentThread().getContextClassLoader, defaultPlugin))
    }
    pluginLoader
  }
}
