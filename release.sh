#!/usr/bin/env bash

echo "deploy scala 2.11"
mlsql_plugin_tool scala211
mvn clean deploy -DskipTests -Prelease-sign-artifacts

echo "deploy scala 2.12"
mlsql_plugin_tool scala212
mvn clean deploy -DskipTests -Prelease-sign-artifacts