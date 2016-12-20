#!/bin/sh

#
# small script to download the release binaries to store in the dist area
# 
# Usage (for default release repo):
# first param is the release verion, second param is optional and a download repository.
# $> ./getrelease.sh 1.1.0 
#  or during a VOTE e.g. :
# $> ./getrelease.sh 1.1.0 https://repository.apache.org/content/repositories/orgapachejohnzon-1016/ 


mkdir -p target/dist/johnzon-$1
cd target/dist/johnzon-$1

if [[ -z $2 ]]; then
  repo_path=https://repository.apache.org/content/groups/public
else
  repo_path=$2
fi

echo "Fetching artifacts from " ${repo_path}
curl -O ${repo_path}/org/apache/johnzon/johnzon/${1}/johnzon-${1}-source-release.zip
curl -O ${repo_path}/org/apache/johnzon/johnzon/${1}/johnzon-${1}-source-release.zip.asc
curl -O ${repo_path}/org/apache/johnzon/johnzon/${1}/johnzon-${1}-source-release.zip.md5
curl -O ${repo_path}/org/apache/johnzon/johnzon/${1}/johnzon-${1}-source-release.zip.sha1

curl -O ${repo_path}/org/apache/johnzon/apache-johnzon/${1}/apache-johnzon-${1}-bin.zip
curl -O ${repo_path}/org/apache/johnzon/apache-johnzon/${1}/apache-johnzon-${1}-bin.zip.asc
curl -O ${repo_path}/org/apache/johnzon/apache-johnzon/${1}/apache-johnzon-${1}-bin.zip.md5
curl -O ${repo_path}/org/apache/johnzon/apache-johnzon/${1}/apache-johnzon-${1}-bin.zip.sha1
