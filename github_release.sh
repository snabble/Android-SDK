#!/bin/bash

if [ -z $GITHUB_ACTIONS ]; then
    echo "This script should only be run by github workflows"
    exit 1
fi

echo "Uploading to maven repository..."

rm -rf build/maven-releases
rm -rf build/maven-snapshots
rm -rf maven-repository

./gradlew publishAllPublicationsToLocalBuildDirRepository

if [ -d "build/maven-releases" ]; then
    git clone --depth 1 -b releases git@github.com:snabble/maven-repository.git maven-repository
fi

if [ -d "build/maven-snapshots" ]; then
    git clone --depth 1 -b snapshots git@github.com:snabble/maven-repository.git maven-repository
fi

cd maven-repository

cp -r ../build/maven-releases/* . 2>/dev/null
cp -r ../build/maven-snapshots/* . 2>/dev/null

git config --global user.email "<>"
git config --global user.name "GitHub Workflows"

git add *
git commit -m "[GitHub-Workflows] release $GITHUB_REF commit $GITHUB_SHA"
git push

