#!/bin/bash

if [ -z ${1+x} ]; then
    echo "no file given"
    exit 1
fi
FILENAME=$1
PASS_VARIABLE_NAME="PASS_$(echo $FILENAME | sed 's/\.gpg$//' | sed s/[^0-9A-Z_]//gi)"
PASS=${!PASS_VARIABLE_NAME}

if [ -z ${PASS} ]; then
    echo "no passphrase found in $PASS_VARIABLE_NAME"
    exit 1
fi

echo $PASS | gpg --passphrase-fd 0 $FILENAME
