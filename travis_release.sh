#!/bin/bash

if [ -z $TRAVIS ]; then
    echo "This script should only be run by Travis-CI"
    exit 1
fi

if git rev-parse "$TRAVIS_TAG" >/dev/null 2>&1; then
    echo "Uploading to maven repository..."

    rm -rf build/maven-releases
    rm -rf build/maven-snapshots
    rm -rf maven-repository

    ./gradlew publishAllPublicationsToLocalBuildDirRepository

    ./travis_decrypt_file.sh github_deploy_key.gpg

    chmod 600 github_deploy_key
    eval $(ssh-agent -s)
    ssh-add github_deploy_key

    if [ -d "build/maven-releases" ]; then
        git clone --depth 1 -b releases git@github.com:snabble/maven-repository.git maven-repository
    fi

    if [ -d "build/maven-snapshots" ]; then
        git clone --depth 1 -b snapshots git@github.com:snabble/maven-repository.git maven-repository
    fi

    cd maven-repository

    cp -r ../build/maven-releases/* . 2>/dev/null
    cp -r ../build/maven-snapshots/* . 2>/dev/null

    git add *
    git commit -m "[Travis-CI] release $TRAVIS_TAG commit $TRAVIS_COMMIT"
    git push
else
    echo "Skipping deployment to maven repository, no tag set"
fi

