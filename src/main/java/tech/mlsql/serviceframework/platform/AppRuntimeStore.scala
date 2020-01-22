package tech.mlsql.serviceframework.platform

import com.fasterxml.jackson.annotation.JsonIgnore
import tech.mlsql.serviceframework.platform.app.StartupPhase
import tech.mlsql.serviceframework.platform.appstore.{InMemoryStore, KVIndex, KVStore}
import tech.mlsql.serviceframework.platform.loader.ClzzLoaderRuntimeStore
import tech.mlsql.serviceframework.platform.runtimestore.{ActionRuntimeStore, CustomAppRuntimeStore, ExceptionRenderRuntimeStore, RequestCleanerRuntimeStore}

class AppRuntimeStore(val store: KVStore, val listener: Option[AppSRuntimeListener] = None)
  extends ExceptionRenderRuntimeStore
    with ActionRuntimeStore with RequestCleanerRuntimeStore
    with ClzzLoaderRuntimeStore with CustomAppRuntimeStore {

}

object AppRuntimeStore {
  private val _store = new InMemoryStore()
  val store = new AppRuntimeStore(_store)

}

class AppSRuntimeListener {}

case class CustomClassItem(@KVIndex name: String, className: String, loader: PluginLoader)

case class ClzzLoaderItem(@KVIndex name: String, loader: PluginLoader) {
  @JsonIgnore
  @KVIndex
  def id = name
}

case class AppItem(name: String, className: String, loader: PluginLoader, phase: Option[StartupPhase]) {
  @JsonIgnore
  @KVIndex
  def id = name
}

case class ActionItem(name: String, className: String, loader: PluginLoader) {
  @JsonIgnore
  @KVIndex
  def id = name
}

case class CustomClassItemWrapper(customClassItem: CustomClassItem) {
  @JsonIgnore
  @KVIndex
  def id = customClassItem.name
}

case class ExceptionRenderItemWrapper(customClassItem: CustomClassItem) {
  @JsonIgnore
  @KVIndex
  def id = customClassItem.name
}

case class RequestCleanerItemWrapper(customClassItem: CustomClassItem) {
  @JsonIgnore
  @KVIndex
  def id = customClassItem.name
}
