#!/bin/bash

d=`date +"%s"`
./gradlew publishToMavenLocal -PversionSuffix=-${d}
./gradlew printVersion -PversionSuffix=-${d}