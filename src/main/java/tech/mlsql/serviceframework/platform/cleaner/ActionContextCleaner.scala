package tech.mlsql.serviceframework.platform.cleaner

import tech.mlsql.serviceframework.platform.action.ActionContext

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
class ActionContextCleaner extends RequestCleaner {
  def run(): Unit = {
    ActionContext.unset
  }
}
