#!/bin/sh
set -e
tmp=/tmp
old_lavender=${CISOTOOLS_HOME}/bin/lavender
new_lavender=target/lavender
testhost=${HOME}/Projects/lavender/walter

run() {
  if [ "$#" -ne 4 ] ; then
    echo "usage: download <name> <groupId> <artifactId> <version>"
    exit 1
  fi
  name=$1
  shift
  groupId=$1
  shift
  artifactId=$1
  shift
  version=$1
  shift
  dir=target/regression-tests/${name}
  mkdir -p ${dir}
  echo
  echo "testing ${name}"
  echo
  echo "* download ..."
  mvn -U dependency:copy "-Dartifact=${groupId}:${artifactId}:${version}:war" "-DoutputDirectory=${tmp}" ${dir}/download.log 2>&1
  mv ${tmp}/${artifactId}-${version}.war ${dir}/test.war
  echo "* old lavender ..."
  ${old_lavenderE} war ${dir}/test.war walter web test.idx
  mv ${testhost}/web ${testhost}/${name}-old
  echo "* new lavender ..."
  ${new_lavender} war ${dir}/test.war walter web test.idx
  mv ${testhost}/web ${testhost}/${name}-new
  echo "* diff ..."
  diff -r --brief ${testhost}/${name}-old ${testhost}/${name}-new ${dir}/diff.log
}

echo "testing $(old_lavender version) vs $(new_lavender)"
rm -rf ${testhost}
run com.ionos.shop upgrade-us 2.3.84

