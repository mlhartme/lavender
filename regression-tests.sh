#!/bin/sh
set -e
tmp=/tmp
old_lavender=${CISOTOOLS_HOME}/bin/lavender
new_lavender=target/lavender
testhost=${HOME}/Projects/lavender/walter

artifact() {
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
  echo "download ${groupId}:${artifactId}"
  mvn -U dependency:copy "-Dartifact=${groupId}:${artifactId}:${version}:war" "-DoutputDirectory=${tmp}" >${tmp}/download.log 2>&1
  file=${tmp}/${artifactId}-${version}.war
  war ${name} ${file} ${file}
}

war() {
  if [ "$#" -ne 3 ] ; then
    echo "usage: run <name> <oldwar> <newwar>"
    exit 1
  fi
  name=$1
  shift
  oldwar=$1
  shift
  newwar=$1
  shift
  dir=target/regression-tests/${name}
  rm -rf ${dir}
  mkdir -p ${dir}
  echo
  echo "testing ${name}"
  echo
  echo "* info old ..."
  ${new_lavender} info ${oldwar}
  echo "* info new ..."
  ${new_lavender} info ${newwar}
  echo "* old lavender ..."
  ${old_lavender} war ${oldwar} walter web test.idx >${dir}/old.log 2>&1
  mv ${testhost}/web ${testhost}/${name}-old
  echo "* new lavender ..."
  ${new_lavender} war ${newwar} walter web test.idx >${dir}/new.log 2>&1
  mv ${testhost}/web ${testhost}/${name}-new
  removeIndexComment ${testhost}/${name}-old/indexes
  removeIndexComment ${testhost}/${name}-new/indexes
  echo "* diff ..."
  if diff -r --brief ${testhost}/${name}-old ${testhost}/${name}-new >${dir}/diff.log 2>&1 ; then
    echo "  ok: $?"
  else
    echo "  failed: $?"
  fi
}

removeIndexComment() {
  if [ "$#" -ne 1 ] ; then
    echo "usage: removeIndexComment <indexes>"
    exit 1
  fi
  indexes=$1
  find ${indexes} -type f -print0 | while IFS= read -r -d $'\0' file; do
    echo "$(tail -n +2 ${file})" > ${file}
  done
}

echo "testing $(${old_lavender} version) vs $(${new_lavender} version)"
rm -rf ${testhost}

## shops

#artifact com.ionos.shop order-ca 3.2.18
#artifact com.ionos.shop order-de 3.2.27
#artifact com.ionos.shop order-es 3.2.20
#artifact com.ionos.shop order-it 3.2.20
#artifact com.ionos.shop order-us 3.2.26
#artifact com.ionos.shop order-fr 3.2.24
#artifact com.ionos.shop order-uk 3.2.26
#artifact com.ionos.shop order-mx 3.2.16

artifact com.oneandone.sales ionos-eu 0.0.63

#artifact com.ionos.shop cloud-de 2.0.13
#artifact com.ionos.shop cloud-it 2.0.9
#artifact com.ionos.shop cloud-ca 2.0.10
#artifact com.ionos.shop cloud-fr 2.0.9
#artifact com.ionos.shop cloud-mx 2.0.9
#artifact com.ionos.shop cloud-uk 2.0.9
#artifact com.ionos.shop cloud-us 2.0.9

#artifact com.ionos.shop telesales-de 3.2.15
#artifact com.ionos.shop telesales-es 3.2.12
#artifact com.ionos.shop telesales-fr 3.2.9
#artifact com.ionos.shop telesales-uk 3.2.12
#artifact com.ionos.shop telesales-us 3.2.8

## cp

#artifact com.ionos.shop upgrade-us 2.3.84
#artifact com.ionos.shop upgrade-es 2.4.84
#artifact com.ionos.shop upgrade-it 2.3.69
#artifact com.ionos.shop upgrade-fr 3.2.85
#artifact com.ionos.shop upgrade-uk 2.4.87
#artifact com.ionos.shop upgrade-de 2.6.91
#artifact com.ionos.shop upgrade-ca 2.4.69
#artifact com.ionos.shop upgrade-mx 2.3.66
