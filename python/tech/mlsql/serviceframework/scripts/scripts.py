from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import subprocess
import sys
import time

import click

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
def create(name):
    command = ["git", "clone", "https://github.com/allwefantasy/baseweb"]
    run_cmd(command)

    run_cmd(["mv", "baseweb", name])
    with open("./{}/pom.xml".format(name)) as f:
        newlines = [line.replace("baseweb_2.11", name + "_2.11") for line in f.readlines()]

    with open("./{}/pom.xml".format(name), "w") as f:
        f.writelines(newlines)

    os.mkdir(os.path.join(".", name, ".sfcli"))
    os.mkdir(os.path.join(".", name, ".sfcli", "cache"))
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
def run(runtime, plugin_name, dev):
    app_runtime_jar = ""
    if "APP_RUNTIME_JAR" in os.environ:
        app_runtime_jar = os.environ["APP_RUNTIME_JAR"]

    if runtime is not None:
        app_runtime_jar = runtime

    if app_runtime_jar == "":
        app_runtime_jar = "http://download.mlsql.tech/app-runtime-1.0.0/app-runtime_2.11-1.0.0.jar "

    cache_path = os.path.join(".sfcli", "cache", "app-runtime_2.11-1.0.0.jar")

    if not os.path.exists(cache_path) and app_runtime_jar.startswith("http://") or app_runtime_jar.startswith(
            "https://"):
        run_cmd(["wget", app_runtime_jar, "-O", cache_path])

    if os.path.exists(cache_path):
        app_runtime_jar = cache_path

    build_class_path = os.path.join(".", "target", "classes")
    if dev:
        plugins = [app_runtime_jar]
        run_cmd(["mvn", "dependency:copy-dependencies", "-DincludeScope=runtime",
                 "-DoutputDirectory=./release/libs"])
        app_runtime_jar = app_runtime_jar + ":" + "./release/libs/*" + ":" + build_class_path
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
    main_class = "tech.mlsql.serviceframework.platform.AppRuntime"
    if plugin_name is None:
        args = "tech.mlsql.app_runtime.plugin.PluginDesc"
    else:
        args = plugin_name

    pluginPaths = ",".join(plugins)
    pluginNames = args
    command = ["java", "-cp", ".:{}".format(app_runtime_jar), main_class,
               "-pluginPaths {} -pluginNames {}".format(pluginPaths, pluginNames)]
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

    monitor_dir(build_class_path, handler)

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
    "--dev",
    required=False,
    type=bool,
    help="enable dev")
def compile(dev):
    if dev:
        print("=======================")
        print("incremental compile... ")
        print("Using Ctrl+C to stop")
        print("=======================")
        run_cmd(["mvn", "scala:cc"])
    else:
        run_cmd(["mvn", "clean", "compile"])


@cli.command()
def release():
    run_cmd(["mvn", "-DskipTests", "clean", "package", "-Pshade"])
    if not os.path.exists("release"):
        os.mkdir("release")
    files = [file for file in os.listdir("target") if
             file.endswith(".jar")
             and not file.endswith("-sources.jar")
             and not file.startswith("original-")]
    for file in files:
        run_cmd(["cp", "-r", "target/{}".format(file), "release"])
    print("done")


cli.add_command(create)
cli.add_command(compile)
cli.add_command(release)
cli.add_command(run)


def main():
    return cli()


if __name__ == "__main__":
    main()
