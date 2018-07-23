#!/bin/bash

echo "Uploading SNAPSHOT to maven repository..."

rm -rf build/maven-snapshots
rm -rf maven-repository

./gradlew clean uploadArchives -PversionSuffix=-SNAPSHOT

if [ -d "build/maven-snapshots" ]; then
    git clone --depth 1 -b snapshots git@github.com:snabble/maven-repository.git maven-repository
fi

cd maven-repository

cp -r ../build/maven-snapshots/* . 2>/dev/null

git add *
COMMIT=`git rev-parse HEAD`
git commit -m "[SNAPSHOT] commit $COMMIT"
git push

