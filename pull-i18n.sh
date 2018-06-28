#!/bin/bash
git clone --depth=1 git@github.com:snabble/SDK-i18n.git i18ntemp

twine generate-all-localization-files i18ntemp/Snabble.twine ui/src/main/res/ --tags android --untagged --format android

rm -rf i18ntemp
git add ui/src/main/res/values/strings.xml