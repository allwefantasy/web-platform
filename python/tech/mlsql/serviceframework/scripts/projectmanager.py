import os
import shutil
import uuid

from tech.mlsql.serviceframework.scripts.shellutils import run_cmd


def clone_project(name):
    command = ["git", "clone", "https://github.com/allwefantasy/baseweb"]
    run_cmd(command)


def change_project_name(name):
    run_cmd(["mv", "baseweb", name])

    run_cmd(["mv", os.path.join(name, "baseweb-bin"),
             os.path.join(name, "{}-bin".format(name))])

    run_cmd(["mv", os.path.join(name, "baseweb-lib"),
             os.path.join(name, "{}-lib".format(name))])


def change_pom(pom_path, name, scala_prefix):
    with open(pom_path) as f:
        newlines = [line.replace("baseweb_" + scala_prefix, name + "_" + scala_prefix) for line in f.readlines()]
        newlines = [line.replace("baseweb-bin", name + "-bin") for line in newlines]
        newlines = [line.replace("baseweb-lib", name + "-lib") for line in newlines]

    with open(pom_path, "w") as f:
        f.writelines(newlines)


def change_all_poms(name, scala_prefix):
    change_pom(os.path.join(".", "{}", "pom.xml").format(name), name, scala_prefix)

    change_pom(os.path.join(".", "{}", "{}-bin", "pom.xml").format(name, name), name, scala_prefix)
    change_pom(os.path.join(".", "{}", "{}-lib", "pom.xml").format(name, name), name, scala_prefix)


def create_cache_dir(name):
    os.mkdir(os.path.join(".", name, ".sfcli"))
    os.mkdir(os.path.join(".", name, ".sfcli", "cache"))


def create_project_file(name):
    with open(os.path.join(".", name, ".sfcli", "projectName"), "w") as f:
        f.writelines([name])


def change_plugin_db_scala_file(name):
    plugin_db_scala = "src/main/scala/tech/mlsql/app_runtime/example/PluginDB.scala".split("/")
    plugin_db_scala_path = os.path.join(".", name, "{}-lib".format(name), *plugin_db_scala)
    with open(plugin_db_scala_path) as f:
        newlines = [item.replace('val plugin_name = "example"', 'val plugin_name = "{}"'.format(name)) for item in
                    f.readlines()]
    with open(plugin_db_scala_path, "w") as f:
        f.writelines(newlines)


def change_package_json():
    import json
    package_json_path = "web_console/package.json"
    with open(package_json_path) as f:
        config = json.load(f)
        config["homepage"] = "./"
        config["proxy"] = "http://127.0.0.1:9007"
    with open(package_json_path, "w") as f:
        json.dump(config, f, indent=4)


def clean_files_for_empty_project(name):
    for item in [".git", "{}-bin".format(name), "{}-lib".format(name), "src"]:
        shutil.rmtree(os.path.join(name, item))
    for item in ["README.md", "pom.xml"]:
        run_cmd(["rm", os.path.join(name, item)])


def generate_admin_token_in_yml(name):
    admin_token = str(uuid.uuid4())
    with open(os.path.join(".", name, "config", "application.yml"), "a") as ayml:
        ayml.writelines(["admin_token: {}".format(admin_token)])
    return admin_token
