import os
import pathlib

from tech.mlsql.serviceframework.scripts.shellutils import run_cmd


def get_app_jar_path(runtime):
    app_runtime_jar = ""
    if "APP_RUNTIME_JAR" in os.environ:
        app_runtime_jar = os.environ["APP_RUNTIME_JAR"]

    if runtime is not None:
        app_runtime_jar = runtime

    if app_runtime_jar == "":
        app_runtime_jar = "http://download.mlsql.tech/app-runtime-1.0.0/app-runtime_2.11-1.0.0.jar "
    return app_runtime_jar


def get_cache_path():
    return os.path.join(".sfcli", "cache", "app-runtime_2.11-1.0.0.jar")


def cache_app_jar(app_runtime_jar):
    cache_path = get_cache_path()
    cache_dir = pathlib.Path(cache_path).parent
    if not cache_dir.exists():
        cache_dir.mkdir()

    def is_http_loc():
        return (app_runtime_jar.startswith("http://") or \
                app_runtime_jar.startswith("https://"))

    if not os.path.exists(cache_path) and is_http_loc():
        run_cmd(["wget", app_runtime_jar, "-O", cache_path])

    if not os.path.exists(cache_path) and not is_http_loc():
        command = ["cp", "-r", app_runtime_jar, cache_path]
        print(" ".join(command))
        run_cmd(command)
