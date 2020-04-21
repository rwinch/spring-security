#!/bin/bash

TO=$1
FROM=boot/helloworld
cp -r $FROM/gradle $TO
cp $FROM/gradle.properties $TO
cp $FROM/settings.gradle $TO
cp $FROM/gradlew $TO
cp $FROM/gradlew.bat $TO
mv $FROM/src/integration-test $TO/src/integTest
