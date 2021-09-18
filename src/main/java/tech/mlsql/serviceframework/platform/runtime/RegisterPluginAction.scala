package tech.mlsql.serviceframework.platform.runtime

import net.csdn.ServiceFramwork
import net.csdn.common.settings.Settings
import tech.mlsql.common.utils.log.Logging
import tech.mlsql.common.utils.serder.json.JSONTool
import tech.mlsql.serviceframework.platform.action.{ActionContext, CustomAction}
import tech.mlsql.serviceframework.platform.app.AppManager
import tech.mlsql.serviceframework.platform.{AppRuntimeStore, PluginLoader, PluginType}

class RegisterPluginAction extends CustomAction with Logging {
  override def run(params: Map[String, String]): String = {
    require(params.contains("admin_token"), "admin token is required")
    if (!canAccess(params("admin_token"))) {
      val context = ActionContext.context()
      render(context.httpContext.response, 400, "admin token is required")
    }
    val url = params("url")
    val className = params("className")
    val loader = PluginLoader.load(Array(url), className)
    val plugin = loader.plugin
    plugin.entries.foreach { pi =>
      if (pi.pluginType == PluginType.app) {
        AppRuntimeStore.store.getApp(pi.name).foreach { appItem =>
          logInfo(s"Plugin: load plugin ${appItem.name}")
          AppManager.call(appItem.name, Map())
        }
      }
    }
    JSONTool.toJsonStr(List())
  }

  def adminToken = {
    ServiceFramwork.injector.getInstance(classOf[Settings]).get("admin_token")
  }

  def canAccess(token: String) = {
    adminToken == token
  }
}
