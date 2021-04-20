#!/bin/sh

TMP=/tmp

download() {
  if [ "$#" -ne 3 ] ; then
    echo "usage: download <groupId> <artifactId> <version>"
    exit 1
  fi
  groupId=$1
  shift
  artifactId=$1
  shift
  version=$1
  shift
  mvn -U dependency:copy "-Dartifact=${groupId}:${artifactId}:${version}:war" "-DoutputDirectory=${TMP}"
  # TODO * because SNAPSHOT is replaced by timestamp
  mv ${TMP}/${artifactId}-${version}.war test.war
}

download com.ionos.shop upgrade-us 2.3.84

rm -rf ${HOME}/Projects/lavender/walter/web
lavender war test.war walter web test.idx
