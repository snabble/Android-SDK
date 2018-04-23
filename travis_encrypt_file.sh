#!/bin/bash

if [ -z ${1+x} ]; then
    echo "no file given"
    exit 1
fi
FILENAME=$1
PASS_VARIABLE_NAME="PASS_$(echo $FILENAME | sed s/[^0-9A-Z_]//gi)"

PASS=$(openssl rand -base64 32)
echo $PASS | gpg --batch --passphrase-fd 0 --symmetric $FILENAME
travis encrypt "${PASS_VARIABLE_NAME}=$PASS" --add
