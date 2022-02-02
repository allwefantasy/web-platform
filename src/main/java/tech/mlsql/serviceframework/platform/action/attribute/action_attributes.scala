package tech.mlsql.serviceframework.platform.action.attribute

/**
 * 29/1/2022 WilliamZhu(allwefantasy@gmail.com)
 */
trait GroupAttribute {
  def groupName(): String
}

trait ModuleAttribute {
  def moduleName():String
}

trait OptionsAttribute {
  def options(): Map[String, String]
}
