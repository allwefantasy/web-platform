from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import sys

import click

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

    print("done")


cli.add_command(create)


def main():
    return cli()


if __name__ == "__main__":
    main()
