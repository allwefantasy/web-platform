#!/usr/bin/env python

from __future__ import print_function

import sys

from setuptools import setup

if sys.version_info < (2, 7):
    print("Python versions prior to 2.7 are not supported for pip installed sfcli.",
          file=sys.stderr)
    sys.exit(-1)

try:
    exec(open('tech/mlsql/serviceframework/version.py').read())
except IOError:
    print("Failed to load sfcli version file for packaging.",
          file=sys.stderr)
    sys.exit(-1)

VERSION = __version__

setup(
    name='sfcli',
    version=VERSION,
    description='web-platform command tools',
    long_description="With this lib help you to develop base on web-platform",
    author='ZhuWilliam',
    author_email='allwefantasy@gmail.com',
    url='https://github.com/allwefantasy/sfcli',
    packages=['tech',
              'tech.mlsql',
              'tech.mlsql.serviceframework',
              'tech.mlsql.serviceframework.scripts',
              ],
    include_package_data=True,
    license='http://www.apache.org/licenses/LICENSE-2.0',
    install_requires=[
        'click>=6.7',
        'py4j==0.10.8.1'],
    entry_points='''
        [console_scripts]
        sfcli=tech.mlsql.serviceframework.scripts.scripts:main
    ''',
    setup_requires=['pypandoc'],
    classifiers=[
        'Development Status :: 5 - Production/Stable',
        'License :: OSI Approved :: Apache Software License',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.4',
        'Programming Language :: Python :: 3.5',
        'Programming Language :: Python :: 3.6',
        'Programming Language :: Python :: 3.7',
        'Programming Language :: Python :: Implementation :: CPython',
        'Programming Language :: Python :: Implementation :: PyPy']
)
