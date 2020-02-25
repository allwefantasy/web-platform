import os

from tech.mlsql.serviceframework.scripts.shellutils import run_cmd

os.chdir("/Users/allwefantasy/CSDNWorkSpace/jack1")
run_cmd("ls -l", shell=True)
run_cmd("cp -r web-ui-template/*  ./", shell=True)
