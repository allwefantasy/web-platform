package tech.mlsql.serviceframework.platform

import com.fasterxml.jackson.annotation.JsonIgnore
import tech.mlsql.serviceframework.platform.appstore.{InMemoryStore, KVIndex, KVStore}
import tech.mlsql.serviceframework.platform.runtimestore.{ControllerRuntimeStore, ExceptionRenderRuntimeStore, RequestCleanerRuntimeStore}

/**
 * 17/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class AppRuntimeStore(val store: KVStore, val listener: Option[AppSRuntimeListener] = None)
  extends ExceptionRenderRuntimeStore
    with ControllerRuntimeStore with RequestCleanerRuntimeStore {

}

object AppRuntimeStore {
  private val _store = new InMemoryStore()
  val store = new AppRuntimeStore(_store)

}

class AppSRuntimeListener {}

case class CustomClassItem(@KVIndex name: String, className: String)

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