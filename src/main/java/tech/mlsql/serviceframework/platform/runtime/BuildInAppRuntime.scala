package tech.mlsql.serviceframework.platform.runtime

import net.csdn.ServiceFramwork
import net.csdn.bootstrap.Application
import tech.mlsql.common.utils.classloader.ClassLoaderTool
import tech.mlsql.common.utils.shell.command.ParamsUtil
import tech.mlsql.serviceframework.platform._
import tech.mlsql.serviceframework.platform.app.{AfterHTTPPhase, AppManager, BeforeHTTPPhase}
import tech.mlsql.serviceframework.platform.plugin.RateLimiterAppPluginDesc

import java.io.File
import scala.collection.JavaConverters._

/**
 * 17/9/2021 WilliamZhu(allwefantasy@gmail.com)
 */
object BuildInAppRuntime {
  def main(args: Array[String], plugins: List[Plugin]): Unit = {
    val ALL_PLUGINS = defaultPlugin ++ plugins
    val params = new ParamsUtil(args)

    val applicationYamlName = params.getParam("config", "application.yml")
    val packageNames = params.getParam("parentLoaderWhiteList", "packageNames.txt")
    val packageFile = new File(packageNames)
    if (packageFile.exists()) {
      scala.io.Source.fromFile(packageFile).getLines().foreach { item =>
        PackageNames.names.add(item)
      }
    }

    ServiceFramwork.applicaionYamlName(applicationYamlName)
    ServiceFramwork.scanService.setLoader(classOf[BuildInAppRuntime])
    ServiceFramwork.enableNoThreadJoin()
    load(ALL_PLUGINS)
    loadPlugin(params)

    AppRuntimeStore.store.getApps().filter(_.phase.head == BeforeHTTPPhase).foreach { appItem =>
      AppManager.call(appItem.name, params.getParamsMap.asScala.toMap)
    }

    Application.main(args)

    AppRuntimeStore.store.getApps().filter(_.phase.head == AfterHTTPPhase).foreach { appItem =>
      AppManager.call(appItem.name, params.getParamsMap.asScala.toMap)
    }
    Thread.currentThread().join()

  }

  def defaultPlugin = {
    List(new RateLimiterAppPluginDesc())
  }

  def load(plugins: List[Plugin]) = {


    plugins.foreach { plugin =>
      val pluginLoader = PluginLoader(Thread.currentThread().getContextClassLoader, plugin)
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

    }

  }

  def loadPlugin(params: ParamsUtil) = {
    if (params.hasParam("pluginPaths")) {
      val paths = params.getParam("pluginPaths").split(",")
      val names = params.getParam("pluginNames").split(",")
      names.zip(paths).map { case (name, path) =>
        PluginLoader.load(Array(path), name)
      }
    }

    val actionName = "registerPlugin"
    if (!AppRuntimeStore.store.getAction(actionName).isDefined) {
      val defaultPlugin = new Plugin() {
        override def entries: List[PluginItem] = {
          List(PluginItem(actionName, classOf[RegisterPluginAction].getName, PluginType.action, None))
        }
      }
      AppRuntimeStore.store.registerAction(actionName, classOf[RegisterPluginAction].getName,
        PluginLoader(ClassLoaderTool.getContextOrDefaultLoader, defaultPlugin))
    }
  }
}

class BuildInAppRuntime {}
