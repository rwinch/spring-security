#!/bin/bash

TO=$1
FROM=javaconfig/inmemory
cp -r $FROM/gradle $TO
cp $FROM/gradle.properties $TO
cp $FROM/settings.gradle $TO
cp $FROM/gradlew $TO
cp $FROM/gradlew.bat $TO
mv $TO/src/integration-test $TO/src/integTest
cp -r $FROM/src/main/java/example $TO/src/main/java
cp -r $FROM/src/main/resources $TO/src/main/
