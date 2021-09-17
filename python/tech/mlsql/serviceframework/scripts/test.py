# import os
# from tech.mlsql.serviceframework.scripts.actionmanager import ActionManager
#
# # # upload app runtime plugin
# action_manager = ActionManager("http://127.0.0.1:9007", "admin", None)
# # action_manager.uploadPlugin("http://127.0.0.1:9007/run",
# #                             "/Users/allwefantasy/rust.sh", {"userName": "jack", "password": "123",
# #                             "pluginName":"rust",
# #                             "version":"1.0.0"})
#
#
# # # upload mlsql plugin
# action_manager.uploadPlugin("http://127.0.0.1:9007/run",
#                             "/Users/allwefantasy/rust.sh", {"userName": "jack", "password": "123",
#                             "pluginType":"MLSQL_PLUGIN",
#                             "pluginName":"mlsql-rust",
#                             "version":"1.0.0",
#                             "mainClass":"tech.mlsql.rust",
#                             "author":"william",
#                             "mlsqlVersions":"1.6.0,1.7.0",
#                             "githubUrl":"http://github",
#                             "desc":"完美rust插件",
#                             })
# action_manager.plugin_cache_path = "/tmp"
# action_manager._downloadPlugin("http://127.0.0.1:9007/run",
#                                     {"action": "downloadPlugin", "pluginName": "rust", "version": "1.0.0","pluginType":"APP_RUNTIME_PLUGIN"})
import requests

p = requests.post("http://127.0.0.1:9003/run/script", {"executeMode": "/watcher/db", "dbName": "app_runtime_full", "dbConfig": """
app_runtime_full:
      host: 127.0.0.1
      port: 3306
      database: app_runtime_full
      username: root
      password: mlsql
      initialSize: 8
      disable: false
      removeAbandoned: true
      testWhileIdle: true
      removeAbandonedTimeout: 30
      maxWait: 100
      filters: stat,log4j
"""})
print(p.text)
