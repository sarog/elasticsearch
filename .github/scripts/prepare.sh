#!/bin/sh

mkdir ~/.gradle

# "org.gradle.jvmargs="
{
  echo "org.gradle.java.installations.paths=/usr/local/openjdk17,/usr/local/openjdk21,/usr/local/openjdk25"
  echo "org.gradle.java.installations.auto-download=false"
  echo "org.gradle.console=plain"
  echo "org.gradle.caching=true"
  echo "org.gradle.daemon=false"
} > ~/.gradle/gradle.properties

{
  echo 'export GRADLE_OPTS=${GRADLE_OPTS:+"$GRADLE_OPTS "}--enable-native-access=ALL-UNNAMED'
} >> ~/.profile
