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

from tech.mlsql.serviceframework.scripts import jarmanager, appruntime
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
    help="is empty project")
def create(name, empty):
    scala_prefix = "2.11"

    command = ["git", "clone", "https://github.com/allwefantasy/baseweb"]
    run_cmd(command)
    run_cmd(["mv", "baseweb", name])

    run_cmd(["mv", os.path.join(name, "baseweb-bin"),
             os.path.join(name, "{}-bin".format(name))])

    run_cmd(["mv", os.path.join(name, "baseweb-lib"),
             os.path.join(name, "{}-lib".format(name))])

    if empty:
        for item in [".git", "{}-bin".format(name), "{}-lib".format(name), "src"]:
            shutil.rmtree(os.path.join(name, item))
        for item in ["README.md", "pom.xml"]:
            run_cmd(["rm", os.path.join(name, item)])
        return

    def change_pom(pom_path):
        with open(pom_path) as f:
            newlines = [line.replace("baseweb_" + scala_prefix, name + "_" + scala_prefix) for line in f.readlines()]
            newlines = [line.replace("baseweb-bin", name + "-bin") for line in newlines]
            newlines = [line.replace("baseweb-lib", name + "-lib") for line in newlines]

        with open(pom_path, "w") as f:
            f.writelines(newlines)

    change_pom(os.path.join(".", "{}", "pom.xml").format(name))

    change_pom(os.path.join(".", "{}", "{}-bin", "pom.xml").format(name, name))
    change_pom(os.path.join(".", "{}", "{}-lib", "pom.xml").format(name, name))

    os.mkdir(os.path.join(".", name, ".sfcli"))
    os.mkdir(os.path.join(".", name, ".sfcli", "cache"))
    with open(os.path.join(".", name, ".sfcli", "projectName"), "w") as f:
        f.writelines([name])

    shutil.rmtree(os.path.join(".", name, ".git"))

    plugin_db_scala = "src/main/scala/tech/mlsql/app_runtime/example/PluginDB.scala".split("/")
    plugin_db_scala_path = os.path.join(".", name, "{}-lib".format(name), *plugin_db_scala)
    with open(plugin_db_scala_path) as f:
        newlines = [item.replace('val plugin_name = "example"', 'val plugin_name = "{}"'.format(name)) for item in
                    f.readlines()]
    with open(plugin_db_scala_path, "w") as f:
        f.writelines(newlines)

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
    type=bool,
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

    lib_build_class_path = os.path.join(".", "{}-lib".format(project_name), "target", "classes")
    bin_build_class_path = os.path.join(".", "{}-bin".format(project_name), "target", "classes")
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

    if add and remove:
        raise Exception("--add and --remove should not specified at the same time")
    if add:
        execute_add_plugin(instance_address, add, token)
    if remove:
        pass


def execute_add_plugin(instance_address, add, token):
    res = requests.post("{}/run".format(instance_address),
                        {"action": "registerPlugin", "url": add, "admin_token": token,
                         "className": appruntime.get_plugin_main_class()})
    print(res.status_code)
    print(res.text)


@cli.command()
@click.option(
    "--runtime_path",
    required=False,
    type=str,
    help="the path of app-runtime")
def runtime(runtime_path):
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
    command = ["java", "-cp", ".:{}".format(app_runtime_jar), main_class]
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
    type=bool,
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
    help="deploy to store ")
@click.option(
    "--password",
    required=False,
    type=str,
    help="deploy to store ")
def release(mvn, install, deploy, user, password):
    project_name = get_project_name()
    if not mvn:
        mvn = "./build/mvn"

    if install:
        install_module = "{}-{}".format(project_name, install)
        command = [mvn, "-DskipTests", "clean", "install", "-pl", install_module, "-am"]
        run_cmd(command)
        print("execute: {}".format(" ".join(command)))
        return

    bin_project = "{}-bin".format(project_name)
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
        uploadPlugin(deploy, "{}/release/{}".format(full_path, file),
                     {"name": user, "password": password, "pluginName": project_name})


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


def main():
    return cli()


if __name__ == "__main__":
    main()
