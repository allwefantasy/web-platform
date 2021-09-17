package tech.mlsql.serviceframework.platform.cleaner

/**
 * 19/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
trait RequestCleaner {
  def run(): Unit

  final def call() = {
    try {
      run()
    } catch {
      case e: Exception => e.printStackTrace()
    }

  }
}
