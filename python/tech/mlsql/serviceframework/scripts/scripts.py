from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import pathlib
import shutil
import subprocess
import sys
import time

import click
import requests

import tech.mlsql.serviceframework.scripts.projectmanager as pm
from tech.mlsql.serviceframework.scripts import jarmanager, appruntime
from tech.mlsql.serviceframework.scripts.actionmanager import ActionManager, DBConfig
from tech.mlsql.serviceframework.scripts.pathmanager import PathManager
from tech.mlsql.serviceframework.scripts.process_info import ProcessInfo
from tech.mlsql.serviceframework.scripts.processutils import *
from tech.mlsql.serviceframework.scripts.shellutils import run_cmd


def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)


@click.group()
@click.version_option()
def cli():
    pass


@cli.command()
@click.option(
    "--name",
    required=False,
    type=str,
    help="the name of project")
@click.option(
    "--empty",
    required=False,
    is_flag=True,
    help="is empty project")
@click.option(
    "--include_ui",
    is_flag=True,
    required=False,
    help="ReactJS project - web_console")
@click.option(
    "--ui_command",
    required=False,
    type=str,
    help="default: create-react-app web_console")
def create(name, empty, include_ui, ui_command):
    scala_prefix = "2.11"

    pm.clone_project(name)
    pm.change_project_name(name)

    pm.generate_admin_token_in_yml(name)

    if empty:
        pm.clean_files_for_empty_project(name)
        return

    pm.change_all_poms(name, scala_prefix)

    pm.create_cache_dir(name)
    pm.create_project_file(name)

    shutil.rmtree(os.path.join(".", name, ".git"))

    pm.change_plugin_db_scala_file(name)

    if include_ui:
        command = "create-react-app web_console"
        if ui_command:
            command = ui_command
        cwd = os.getcwd()
        os.chdir(os.path.join(name))
        run_cmd(command)
        pm.change_package_json()
        # run_cmd(["npm", "i", "@allwefantasy/web-platform-ui", "--save"])
        run_cmd("cp -r web-ui-template/*  web_console/src/", shell=True)
        run_cmd("rm -rf web-ui-template", shell=True)
        os.chdir(cwd)

    print("done")


@cli.command()
@click.option(
    "--runtime",
    required=False,
    type=str,
    help="the path of app-runtime")
@click.option(
    "--plugin_name",
    required=False,
    type=str,
    help="the full package name of class")
@click.option(
    "--dev",
    required=False,
    is_flag=True,
    help="enable dev")
@click.option(
    "--mvn",
    required=False,
    type=str,
    help="mvn command")
@click.option(
    "--debug_port",
    required=False,
    type=int,
    help="debug port")
def run(runtime, plugin_name, dev, mvn, debug_port):
    project_name = get_project_name()

    if not mvn:
        mvn = "./build/mvn"

    app_runtime_jar = jarmanager.get_app_jar_path(runtime)

    cache_path = jarmanager.get_cache_path()

    jarmanager.cache_app_jar(app_runtime_jar)

    if os.path.exists(cache_path):
        app_runtime_jar = cache_path

    path_manager = PathManager(project_name)
    lib_build_class_path = path_manager.lib_classes()
    bin_build_class_path = path_manager.bin_classes()
    # dependencies_output_path = os.path.join(".", "release", "libs")
    if dev:
        run_cmd([mvn, "-DskipTests", "install", "-pl", "{}-lib".format(project_name), "-am"])
        run_cmd([mvn, "-DskipTests", "compile", "-pl", "{}-bin".format(project_name)])
        plugins = [app_runtime_jar]
        # run_cmd(["mvn", "dependency:copy-dependencies", "-DincludeScope=runtime",
        #          "-DoutputDirectory={}".format(dependencies_output_path)], "-fn")
        class_path_str_file = os.path.join(".sfcli", ".classpath")
        run_cmd([mvn, "dependency:build-classpath", "-Dmdep.outputFile={}".format(class_path_str_file)])
        with open(class_path_str_file, "r") as f:
            class_path_str = f.readlines()[0].strip("\n")
        app_runtime_jar = app_runtime_jar + ":" + class_path_str + ":" + lib_build_class_path
    else:
        plugins = ["./release/{}".format(jarName) for jarName in os.listdir("release") if jarName.endswith(".jar")]
    try:
        os.setpgrp()
    except OSError as e:
        eprint("setpgrp failed, processes may not be "
               "cleaned up properly: {}.".format(e))
        # Don't start the reaper in this case as it could result in killing
        # other user processes.
        return None
    main_class = appruntime.get_app_runtime_main_class()
    if plugin_name is None:
        args = appruntime.get_plugin_main_class()
    else:
        args = plugin_name

    pluginPaths = ",".join(plugins)
    pluginNames = args

    debug_args = ""
    if dev:
        debug_args = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address={}".format(str(debug_port))
    command = ["java", "-Xms2g", debug_args, "-cp", ".:{}".format(app_runtime_jar), main_class,
               "-pluginPaths {} -pluginNames {}".format(bin_build_class_path, pluginNames)]
    print("start:{}".format(" ".join(command)))

    def block_sigint():
        import signal
        signal.pthread_sigmask(signal.SIG_BLOCK, {signal.SIGINT})

    modified_env = os.environ.copy()
    cwd = None

    # stdout_file, stderr_file = new_log_files("sfcli-logs")
    pipe_stdin = False

    def start_process():
        wow = subprocess.Popen(
            command,
            env=modified_env,
            cwd=cwd,
            stdout=sys.stdout,
            stderr=sys.stderr,
            stdin=subprocess.PIPE if pipe_stdin else None,
            preexec_fn=block_sigint)
        return wow

    ProcessInfo.inner_process = start_process()

    def handler(event):
        ProcessInfo.event_buffer.append(event)

    monitor_dir(lib_build_class_path, handler)

    while True:
        time.sleep(1)
        if len(ProcessInfo.event_buffer) > 0:
            print("Restarting....")
            time.sleep(3)
            ProcessInfo.event_buffer.clear()
            ProcessInfo.kill()
            count = 10
            while ProcessInfo.inner_process is None and count > 0:
                count -= 1
                try:
                    ProcessInfo.inner_process = start_process()
                except Exception as e:
                    time.sleep(1)
    ProcessInfo.kill()


@cli.command()
@click.option(
    "--add",
    required=False,
    type=str,
    help="jar path")
@click.option(
    "--remove",
    required=False,
    type=str,
    help="jar path")
@click.option(
    "--instance_address",
    required=False,
    type=str,
    help="the runtime url path,default http://127.0.0.1:9007")
@click.option(
    "--token",
    required=False,
    type=str,
    help="admin token")
def plugin(add, remove, instance_address, token):
    if not instance_address:
        instance_address = "http://127.0.0.1:9007"
    actionManager = ActionManager(instance_address, token, None)
    if add and remove:
        raise Exception("--add and --remove should not specified at the same time")
    if add:
        actionManager.installPluginFromUrl(add)
    if remove:
        pass


@cli.command()
@click.option(
    "--runtime_path",
    required=False,
    type=str,
    help="the path of app-runtime")
@click.option(
    "--max_memory",
    required=False,
    type=str,
    help="memory")
def runtime(runtime_path, max_memory):
    try:
        os.setpgrp()
    except OSError as e:
        eprint("setpgrp failed, processes may not be "
               "cleaned up properly: {}.".format(e))
        # Don't start the reaper in this case as it could result in killing
        # other user processes.
        return None
    main_class = appruntime.get_app_runtime_main_class()
    app_runtime_jar = jarmanager.get_app_jar_path(runtime_path)
    jarmanager.cache_app_jar(app_runtime_jar)
    java_args = []
    if max_memory:
        java_args.append("-Xmx{}".format(max_memory))

    command = ["java"] + java_args + ["-cp", ".:{}".format(app_runtime_jar), main_class]
    print("start:{}".format(" ".join(command)))

    def block_sigint():
        import signal
        signal.pthread_sigmask(signal.SIG_BLOCK, {signal.SIGINT})

    modified_env = os.environ.copy()
    cwd = None

    # stdout_file, stderr_file = new_log_files("sfcli-logs")
    pipe_stdin = False

    def start_process():
        wow = subprocess.Popen(
            command,
            env=modified_env,
            cwd=cwd,
            stdout=sys.stdout,
            stderr=sys.stderr,
            stdin=subprocess.PIPE if pipe_stdin else None,
            preexec_fn=block_sigint)
        return wow

    ProcessInfo.inner_process = start_process()

    while True:
        time.sleep(5)

    ProcessInfo.kill()


@cli.command()
@click.option(
    "--dev",
    required=False,
    is_flag=True,
    help="enable dev")
@click.option(
    "--mvn",
    required=False,
    type=str,
    help="mvn command")
@click.option(
    "--pl",
    required=False,
    type=str,
    help="module name")
def compile(dev, mvn, pl):
    if not mvn:
        mvn = "./build/mvn"
    project_name = get_project_name()
    # run_cmd("mvn", "-DskipTests", "clean", "install")

    if not pl:
        pl = "{}-lib".format(project_name)

    if pl:
        os.chdir(os.path.join(".", pl))
        mvn = mvn.replace(".", "..")
    if dev:
        print("=======================")
        print("incremental compile... ")
        print("Using Ctrl+C to stop")
        print("=======================")
        run_cmd([mvn, "clean", "scala:cc", "-Dfsc=false"])
    else:
        run_cmd([mvn, "clean", "compile"])


@cli.command()
@click.option(
    "--mvn",
    required=False,
    type=str,
    help="mvn command")
@click.option(
    "--install",
    required=False,
    type=str,
    help="install bin or lib ")
@click.option(
    "--deploy",
    required=False,
    type=str,
    help="deploy to store ")
@click.option(
    "--user",
    required=False,
    type=str,
    envvar='STORE_USER',
    help="deploy to store ")
@click.option(
    "--password",
    required=False,
    type=str,
    envvar='STORE_PASSWORD',
    help="deploy to store ")
@click.option(
    "--skip_ui",
    required=False,
    type=bool,
    help="include ui")
def release(mvn, install, deploy, user, password, skip_ui):
    project_name = get_project_name()
    actionManager = ActionManager(None, None, None)
    if not mvn:
        mvn = "./build/mvn"

    bin_project = "{}-bin".format(project_name)
    if not skip_ui and os.path.exists("web_console"):
        resource_dir = os.path.join(bin_project, "src", "main", "resources", project_name, project_name)
        if os.path.exists(resource_dir):
            shutil.rmtree(resource_dir)
        if not os.path.exists(resource_dir):
            run_cmd(["mkdir", "-p", resource_dir])

        cwd = os.getcwd()
        os.chdir("./web_console")
        run_cmd(["npm", "run", "build"])
        os.chdir(cwd)
        run_cmd(["rm", "-rf", os.path.join(resource_dir, "*")])
        run_cmd("cp -r {} {}".format(os.path.join("web_console", "build", "*"), resource_dir), shell=True)

    if install:
        install_module = "{}-{}".format(project_name, install)
        command = [mvn, "-DskipTests", "clean", "install", "-pl", install_module, "-am"]
        run_cmd(command)
        print("execute: {}".format(" ".join(command)))
        return

    command = [mvn, "-DskipTests", "clean", "package", "-Pshade", "-pl", bin_project, "-am"]
    print("execute: {}".format(" ".join(command)))
    run_cmd(command)
    if os.path.exists("release"):
        shutil.rmtree("release")
    os.mkdir("release")

    files = [file for file in os.listdir(os.path.join(bin_project, "target")) if
             file.endswith(".jar")
             and not file.endswith("-sources.jar")
             and not file.startswith("original-")
             and not file.endswith("-javadoc.jar")]
    for file in files:
        run_cmd(["cp", "-r", "{}/target/{}".format(bin_project, file), "release"])
    full_path = pathlib.Path().absolute()
    print("{}/release/{}".format(full_path, file))
    if deploy:
        if deploy == "store":
            deploy = "http://store.mlsql.tech/run"
        actionManager.uploadPlugin(deploy, "{}/release/{}".format(full_path, file),
                                   {"name": user, "password": password, "pluginName": project_name})


@cli.command()
@click.option(
    "--plugin_name",
    required=True,
    type=str,
    help="plugin name")
@click.option(
    "--db_name",
    required=True,
    type=str,
    help="")
@click.option(
    "--host",
    required=True,
    type=str,
    help="")
@click.option(
    "--port",
    required=False,
    type=int,
    help="")
@click.option(
    "--user",
    required=True,
    type=str,
    help="")
@click.option(
    "--password",
    required=True,
    type=str,
    help="")
@click.option(
    "--instance_address",
    required=False,
    type=str,
    help="the runtime url path,default http://127.0.0.1:9007")
@click.option(
    "--token",
    required=False,
    type=str,
    help="admin token")
def add_db(plugin_name, db_name, host, port, user, password, instance_address, token):
    if not instance_address:
        instance_address = "http://127.0.0.1:9007"
    if not port:
        port = 3306
    action_manager = ActionManager(instance_address, token, None)
    r = action_manager.addDB(plugin_name, DBConfig(db_name, host, port, user, password))
    print(r.status_code)
    print(r.text)


@cli.command()
@click.option(
    "--plugin_name",
    required=True,
    type=str,
    help="plugin name")
@click.option(
    "--url",
    required=True,
    type=str,
    help="the rest api ")
@click.option(
    "--instance_address",
    required=False,
    type=str,
    help="the runtime url path,default http://127.0.0.1:9007")
@click.option(
    "--token",
    required=False,
    type=str,
    help="admin token")
def add_action_proxy(plugin_name, url, instance_address, token):
    if not instance_address:
        instance_address = "http://127.0.0.1:9007"
    action_manager = ActionManager(instance_address, token, None)
    action_manager.addProxy(plugin_name, url)
    pass


def printRespose(r):
    print(r.text)
    print(r.status_code)


def uploadPlugin(storePath, file_path, data):
    values = {**data, **{"action": "uploadPlugin"}}
    files = {file_path.split("/")[-1]: open(file_path, 'rb')}
    r = requests.post(storePath, files=files, data=values)
    printRespose(r)


cli.add_command(create)
cli.add_command(compile)
cli.add_command(release)
cli.add_command(run)
cli.add_command(plugin)
cli.add_command(runtime)
cli.add_command(add_db)
cli.add_command(add_action_proxy)


def main():
    return cli()


if __name__ == "__main__":
    main()
