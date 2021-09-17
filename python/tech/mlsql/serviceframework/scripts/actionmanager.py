import os

import requests

from tech.mlsql.serviceframework.scripts import appruntime
from tech.mlsql.serviceframework.scripts.shellutils import run_cmd


class DBConfig(object):
    def __init__(self, db_name, db_host, db_port, db_user, db_password):
        self.db_name = db_name
        self.db_host = db_host
        self.db_port = db_port
        self.db_user = db_user
        self.db_password = db_password


class ActionManager(object):
    def __init__(self, instance_url, token, plugin_cache_path=None):
        self.instance_url = instance_url
        self.admin_token = token
        self.plugin_cache_path = plugin_cache_path
        if self.plugin_cache_path is None:
            self.plugin_cache_path = os.path.join(".sfcli", "plugins")

    def db_config_template(self, db_config: DBConfig):
        return """
        {}:
              host: {}
              port: {}
              database: {}
              username: {}
              password: {}
              initialSize: 8
              disable: true
              removeAbandoned: true
              testWhileIdle: true
              removeAbandonedTimeout: 30
              maxWait: 100
              filters: stat,log4j
        """.format(db_config.db_name,
                   db_config.db_host,
                   db_config.db_port,
                   db_config.db_name,
                   db_config.db_user,
                   db_config.db_password)

    def http(self, action, params):
        datas = {**{"action": action}, **params}
        r = requests.post("{}/run".format(self.instance_url), data=datas)
        return r

    def addDB(self, instanceName, db_config: DBConfig):
        # user-system
        datas = {"dbName": db_config.db_name, "instanceName": instanceName,
                 "dbConfig": self.db_config_template(db_config), "admin_token": self.admin_token}
        r = self.http("addDB", datas)
        return r

    def downloadPlugin(self, plugin_cache_path, pluginName, version):
        return self._downloadPlugin("http://store.mlsql.tech/run",
                                    {"action": "downloadPlugin", "pluginName": pluginName, "version": version})

    def installPluginFromCache(self, plugin_cache_path):
        res = requests.post("{}/run".format(self.instance_url),
                            {"action": "registerPlugin", "url": plugin_cache_path, "admin_token": self.admin_token,
                             "className": appruntime.get_plugin_main_class()})

        print(res.status_code)
        print(res.text)

    def _downloadPlugin(self, url, params):
        r = requests.get(url, params, stream=True)
        try:
            r.raise_for_status()
        except Exception as exc:
            print('There was a problem: %s' % (exc))
        import re
        import os
        d = r.headers['content-disposition']
        fname = re.findall("filename=(.+)", d)[0].strip('"')
        with open(os.path.join(self.plugin_cache_path, fname), 'wb') as tmp_file:
            for chunk in r.iter_content(100000):
                tmp_file.write(chunk)
        return os.path.join(self.plugin_cache_path, fname)

    def installPluginFromUrl(self, add):

        if not os.path.exists(self.plugin_cache_path):
            os.mkdir(self.plugin_cache_path)

        plugin_url = add

        if (not plugin_url.endswith(".jar")) and not (":" in plugin_url):
            raise Exception(
                "The parameter in --add should like be [plugin_name]:[version] e.g. app_runtime_with_db:1.0.0")

        if (not plugin_url.endswith(".jar")) and (":" in plugin_url):
            plugin_name, version = plugin_url.split(":")
            plugin_jar_cache_path = self.downloadPlugin(self.plugin_cache_path, plugin_name, version)
            plugin_url = plugin_jar_cache_path

        elif plugin_url.startswith("http://") or plugin_url.startswith("https://"):
            plugin_url = self._downloadPlugin(plugin_url, {})
        else:
            run_cmd(["cp", "-r", plugin_url, self.plugin_cache_path])
            plugin_url = os.path.join(self.plugin_cache_path, plugin_url.split(os.path.pathsep)[-1])

        self.installPluginFromCache(plugin_url)

    def uploadPlugin(self, storePath, file_path, data):
        values = {**data, **{"action": "uploadPlugin"}}
        files = {file_path.split("/")[-1]: open(file_path, 'rb')}
        r = requests.post(storePath, files=files, data=values)
        print(r.status_code)
        print(r.text)

    def addProxy(self, name, value):
        r = self.http("addProxy", {"name": name, "value": value, "admin_token": self.admin_token})
        print(r.status_code)
        print(r.text)
