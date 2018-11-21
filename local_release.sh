#!/bin/bash

d=`date +"%s"`
./gradlew install -PversionSuffix=-${d}
./gradlew printVersion -PversionSuffix=-${d}