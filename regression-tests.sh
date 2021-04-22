#!/bin/sh
set -e
tmp=/tmp
old_lavender=${CISOTOOLS_HOME}/bin/lavender
new_lavender=target/lavender
testhost=${HOME}/Projects/lavender/walter

run() {
  if [ "$#" -ne 3 ] ; then
    echo "usage: run <groupId> <artifactId> <version>"
    exit 1
  fi
  groupId=$1
  shift
  artifactId=$1
  shift
  version=$1
  shift
  name=${artifactId}
  dir=target/regression-tests/${name}
  rm -rf ${dir}
  mkdir -p ${dir}
  echo
  echo "testing ${name}"
  echo
  echo "* download ..."
  mvn -U dependency:copy "-Dartifact=${groupId}:${artifactId}:${version}:war" "-DoutputDirectory=${tmp}" >${dir}/download.log 2>&1
  mv ${tmp}/${artifactId}-${version}.war ${dir}/test.war
  echo "* old lavender ..."
  ${old_lavender} war ${dir}/test.war walter web test.idx >${dir}/old.log 2>&1
  mv ${testhost}/web ${testhost}/${name}-old
  echo "* new lavender ..."
  ${new_lavender} war ${dir}/test.war walter web test.idx >${dir}/new.log 2>&1
  mv ${testhost}/web ${testhost}/${name}-new
  removeIndexComment ${testhost}/${name}-old/indexes
  removeIndexComment ${testhost}/${name}-new/indexes
  echo "* diff ..."
  if diff -r --brief ${testhost}/${name}-old ${testhost}/${name}-new >${dir}/diff.log 2>&1 ; then
    echo "  failed: $?"
  else
    echo "  ok"
  fi
}

removeIndexComment() {
  if [ "$#" -ne 1 ] ; then
    echo "usage: removeIndexComment <dir>"
    exit 1
  fi
  dir=$1
  echo "remove ${dir}"
  find ${dir} -type f -print0 | while IFS= read -r -d $'\0' file; do
    echo "$file"
    echo "$(tail -n +2 ${file})" > ${file}
  done
}

echo "testing $(${old_lavender} version) vs $(${new_lavender} version)"
rm -rf ${testhost}

run com.ionos.shop order-ca 3.2.18
run com.ionos.shop order-de 3.2.27
run com.ionos.shop order-es 3.2.20
run com.ionos.shop order-it 3.2.20
run com.ionos.shop order-us 3.2.26
run com.ionos.shop order-fr 3.2.24
run com.ionos.shop order-uk 3.2.26
run com.ionos.shop order-mx 3.2.16

run com.oneandone.sales ionos-eu 0.0.63

run com.ionos.shop upgrade-us 2.3.84
run com.ionos.shop upgrade-es 2.4.84
run com.ionos.shop upgrade-it 2.3.69
run com.ionos.shop upgrade-fr 3.2.85
run com.ionos.shop upgrade-uk 2.4.87
run com.ionos.shop upgrade-de 2.6.91
run com.ionos.shop upgrade-ca 2.4.69
run com.ionos.shop upgrade-mx 2.3.66

run com.ionos.shop cloud-de 2.0.13
run com.ionos.shop cloud-it 2.0.9
run com.ionos.shop cloud-ca 2.0.10
run com.ionos.shop cloud-fr 2.0.9
run com.ionos.shop cloud-mx 2.0.9
run com.ionos.shop cloud-uk 2.0.9
run com.ionos.shop cloud-us 2.0.9

run com.ionos.shop telesales-de 3.2.15
run com.ionos.shop telesales-es 3.2.12
run com.ionos.shop telesales-fr 3.2.9
run com.ionos.shop telesales-uk 3.2.12
run com.ionos.shop telesales-us 3.2.8
